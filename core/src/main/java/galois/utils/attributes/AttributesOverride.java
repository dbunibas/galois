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
    private final Integer pinInPosition;

    public AttributesOverride(List<Attribute> extraAttributes) {
        this.extraAttributes = extraAttributes;
        this.pinnedAttributes = null;
        this.pinInPosition = null;
    }

    public AttributesOverride(List<Attribute> extraAttributes, Integer pinInPosition) {
        this.extraAttributes = extraAttributes;
        this.pinnedAttributes = null;
        this.pinInPosition = pinInPosition;
    }

    @Override
    public String toString() {
        String string = "AttributesOverride " + extraAttributes.size();
        if (pinInPosition != null) {
            string += " - pinned " + pinInPosition;
        }
        return string;
    }
}
