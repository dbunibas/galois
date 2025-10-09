package galois.utils.attributes;

import lombok.AllArgsConstructor;
import lombok.Data;
import speedy.model.database.Attribute;

import java.util.List;

@Data
@AllArgsConstructor
public class AttributesOverride {
    private final List<Attribute> extraAttributes;
    private final List<PinnedAttribute> pinnedAttributes;

    public AttributesOverride(List<Attribute> extraAttributes) {
        this.extraAttributes = extraAttributes;
        this.pinnedAttributes = null;
    }

    @Override
    public String toString() {
        String string = "AttributesOverride " + extraAttributes.size();
        if (pinnedAttributes != null) {
            string += " - pinned " + pinnedAttributes.size();
        }
        return string;
    }
}
