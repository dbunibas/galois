package queryexecutor.persistence.encoding;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queryexecutor.QueryExecutorConstants;
import queryexecutor.exceptions.DAOException;
import queryexecutor.model.database.operators.dbms.IValueEncoder;
import queryexecutor.utility.QueryExecutorUtility;

public class DictionaryEncoder implements IValueEncoder {

    private final static Logger logger = LoggerFactory.getLogger(DictionaryEncoder.class);
    private final String scenarioName;
    private WritingThread writingThread = null;
    private boolean writingInProgress = false;
    private long lastValue = 0;
    private Map<String, Long> encodingMap;
    private Map<Long, String> decodingMap;

    private Lock lock = new java.util.concurrent.locks.ReentrantLock();

    public DictionaryEncoder(String scenarioName) {
        this.scenarioName = scenarioName;
    }

    public String encode(String original) {
        try {
            lock.lock();
            if (encodingMap == null) {
                loadEncodingMap();
            }
            Long encoded = encodingMap.get(original);
            if (encoded == null) {
                encoded = nextValue();
                encodingMap.put(original, encoded);
            }
            if (original.equals("")) {

            }
            return encoded + "";
        } finally {
            lock.unlock();
        }
    }

    public String decode(String encoded) {
        if(decodingMap==null){
            throw new IllegalArgumentException("Decoding map not loaded");
        }
        try {
            lock.lock();
            return decodeValueUsingCache(encoded);
        } finally {
            lock.unlock();
        }
    }

    private String decodeValueUsingCache(String encoded) {
        Long encodedValue;
        try {
            encodedValue = Long.parseLong(encoded);
        } catch (NumberFormatException nfe) {
            throw new DAOException("Unable to decode string value " + encoded);
        }
        String decoded = decodingMap.get(encodedValue);
        if (decoded == null) {
            if (QueryExecutorUtility.isSkolem(encoded) || QueryExecutorUtility.isVariable(encoded)) {
                return encoded;
            }
            throw new DAOException("Unable to decode value " + encodedValue + "\n Current map:" + QueryExecutorUtility.printMap(decodingMap));
        }
        return decoded;
    }

    private Long nextValue() {
        lastValue++;
        if (lastValue < QueryExecutorConstants.MIN_BIGINT_SKOLEM_VALUE && lastValue < QueryExecutorConstants.MIN_BIGINT_LLUN_VALUE) {
            return lastValue;
        }
        String stringLastValue = lastValue + "";
        if (stringLastValue.startsWith(QueryExecutorConstants.BIGINT_SKOLEM_PREFIX) || stringLastValue.startsWith(QueryExecutorConstants.BIGINT_LLUN_PREFIX)) {
            lastValue += QueryExecutorConstants.MIN_BIGINT_SAFETY_SKIP_VALUE;
        }
        if (logger.isTraceEnabled()) {
            if (QueryExecutorUtility.isSkolem(lastValue + "") || QueryExecutorUtility.isVariable(lastValue + "")) throw new IllegalArgumentException("Dictionary encoder generates a skolem or variable " + lastValue);
            if (lastValue % 1000000L == 0) logger.trace("Next value: " + lastValue);
        }
        return lastValue;
    }

    public void prepareForEncoding() {
        encodingMap = null;
    }

    public void removeExistingEncoding() {
        File mapFile = new File(getFileForEncoding());
        if(mapFile.exists()){
            mapFile.delete();
        }
    }

    private void loadEncodingMap() {
        File mapFile = new File(getFileForEncoding());
        if (!mapFile.canRead()) {
            encodingMap = Collections.synchronizedMap(new HashMap<String, Long>());
            return;
        }
        loadMapFromFile(mapFile);
    }

    @SuppressWarnings("unchecked")
    private void loadMapFromFile(File mapFile) {
        ObjectInputStream inStream = null;
        try {
            inStream = new ObjectInputStream(new FileInputStream(mapFile));
            int size = inStream.readInt();
            encodingMap = Collections.synchronizedMap(new HashMap<String, Long>(size));
            for (int i = 0; i < size; i++) {
                String key = inStream.readUTF();
                Long value = inStream.readLong();
                encodingMap.put(key, value);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new DAOException("Unable to load map from file " + mapFile + ".\n" + e.getLocalizedMessage());
        } finally {
            try {
                if (inStream != null) {
                    inStream.close();
                }
            } catch (IOException ioe) {
            }
        }
    }

    public void closeEncoding() {
        try {
            lock.lock();
            if (encodingMap == null) {
                return;
            }
            writingThread = new WritingThread(encodingMap, getFileForEncoding());
            writingThread.start();
            encodingMap = null;
        } finally {
            lock.unlock();
        }
    }

    public void prepareForDecoding() {
        try {
            lock.lock();
            if (writingInProgress) {
                try {
                    writingThread.join();
                } catch (InterruptedException ex) {
                }
            }
            File mapFile = new File(getFileForEncoding());
            if (!mapFile.canRead()) {
                throw new DAOException("Unable to load encoding map file " + mapFile);
            }
            ObjectInputStream inStream = null;
            try {
                inStream = new ObjectInputStream(new FileInputStream(mapFile));
                int size = inStream.readInt();
                decodingMap = Collections.synchronizedMap(new HashMap<Long, String>(size));
                for (int i = 0; i < size; i++) {
                    String key = inStream.readUTF();
                    Long value = inStream.readLong();
                    if (logger.isTraceEnabled()) {
                        if (decodingMap.containsKey(value)) {
                            throw new IllegalArgumentException("Value " + value + " for key " + key + " was already used");
                        }
                    }
                    decodingMap.put(value, key);
                }
            } catch (Exception e) {
                throw new DAOException("Unable to load map from file " + mapFile + ".\n" + e.getLocalizedMessage());
            } finally {
                try {
                    if (inStream != null) {
                        inStream.close();
                    }
                } catch (IOException ioe) {
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public void closeDecoding() {
    }

    private String getFileForEncoding() {
        String homeDir = System.getProperty("user.home");
        return homeDir + File.separator + QueryExecutorConstants.WORK_DIR + File.separator + "Encoding" + File.separator + "MAP_" + scenarioName + ".map";
    }

    public void waitingForEnding() {
        if (writingInProgress) {
            try {
                writingThread.join();
            } catch (InterruptedException ex) {
            }
        }
    }

    class WritingThread extends Thread {

        private Map<String, Long> mapToWrite;
        private String fileToWrite;

        public WritingThread(Map<String, Long> mapToWrite, String fileToWrite) {
            this.mapToWrite = mapToWrite;
            this.fileToWrite = fileToWrite;
        }

        @Override
        public void run() {
            writingInProgress = true;
            ObjectOutputStream out = null;
            try {
                File mapFile = new File(fileToWrite);
                mapFile.getParentFile().mkdirs();
                out = new ObjectOutputStream(new FileOutputStream(mapFile));
                out.writeInt(mapToWrite.size());
                for (String key : mapToWrite.keySet()) {
                    Long value = mapToWrite.get(key);
                    out.writeUTF(key);
                    out.writeLong(value);
                }
                out.close();
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new DAOException("Unable to write dictionary to file " + fileToWrite + ".\n" + ex.getLocalizedMessage());
            } finally {
                try {
                    if (out != null) out.close();
                } catch (IOException ex) {
                }
                writingInProgress = false;
            }
        }

    }

}
