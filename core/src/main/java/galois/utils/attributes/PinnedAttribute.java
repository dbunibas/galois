package galois.utils.attributes;

import lombok.Data;

@Data
public class PinnedAttribute {
    private final String attributeName;
    private final int position;
}
