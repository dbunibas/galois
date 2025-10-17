package queryexecutor.test.utility;

import queryexecutor.utility.Size;

public class TestId implements Comparable<TestId> {

    private Size size;
    private String sizeString;
    private String group;

    public TestId(Size size, String group) {
        this.size = size;
        this.group = group;
    }

    public TestId(String sizeString, String group) {
        this.sizeString = sizeString;
        this.group = group;
    }

    public int compareTo(TestId o) {
        if (this.group.equals(o.group)) {
            if (size == null) {
                return sizeString.compareTo(o.sizeString);
            } else {
                return size.compareTo(o.size);
            }
        }
        return group.compareTo(o.group);
    }

    @Override
    public String toString() {
        return (size == null ? sizeString : size) + " [" + group + ']';
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final TestId other = (TestId) obj;
        return this.toString().equals(other.toString());
    }

}
