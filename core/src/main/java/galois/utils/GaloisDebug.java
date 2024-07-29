
package galois.utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GaloisDebug {
    
    public static void log(Object obj) {
        if (obj != null) log.trace("{}", obj);
    }
}
