package queryexecutor.utility;

public class Size implements Comparable<Size> {

    public static final Size S_1 = new Size(1, "1");
    public static final Size S_5 = new Size(5, "5");
    public static final Size S_1K = new Size(1000, "1k");
    public static final Size S_2K = new Size(2000, "2k");
    public static final Size S_5K = new Size(5000, "5k");
    public static final Size S_7K = new Size(7000, "7k");
    public static final Size S_10K = new Size(10000, "10k");
    public static final Size S_20K = new Size(20000, "20k");
    public static final Size S_30K = new Size(20000, "30k");
    public static final Size S_40K = new Size(20000, "40k");
    public static final Size S_50K = new Size(50000, "50k");
    public static final Size S_70K = new Size(70000, "70k");
    public static final Size S_100K = new Size(100000, "100k");
    public static final Size S_200K = new Size(200000, "200k");
    public static final Size S_250K = new Size(250000, "250k");
    public static final Size S_400K = new Size(400000, "400k");
    public static final Size S_700K = new Size(800000, "700k");
    public static final Size S_800K = new Size(800000, "800k");
    public static final Size S_500K = new Size(500000, "500k");
    public static final Size S_1000K = new Size(500000, "1000k");
    public static final Size S_1M = new Size(1000000, "1M");
    private int size;
    private String string;

    public Size(int size, String string) {
        this.size = size;
        this.string = string;
    }

    public int getSize() {
        return size;
    }

    @Override
    public String toString() {
        return string;
    }

    public int compareTo(Size o) {
        return this.size - o.size;
    }
}
