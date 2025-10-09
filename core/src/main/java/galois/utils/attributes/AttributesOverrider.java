package galois.utils.attributes;

import speedy.model.database.Attribute;

import java.util.*;

public final class AttributesOverrider {
    public static List<Attribute> overrideAttributes(AttributesOverride override, Set<Attribute> attributes) {
        List<Attribute> result = new ArrayList<>(attributes);
        result.addAll(override.getExtraAttributes());
        if (override.getPinnedAttributes() == null) return result;

        for (PinnedAttribute position : override.getPinnedAttributes()) {
            if (position.getPosition() >= result.size()) continue;
            int index = findAttributeIndex(result, position.getAttributeName());
            Collections.swap(result, index, position.getPosition());
        }

        return result;
    }

    private static int findAttributeIndex(List<Attribute> attributes, String name) {
        for (int i = 0; i < attributes.size(); i++) {
            Attribute attribute = attributes.get(i);
            if (attribute.getName().equals(name)) return i;
        }
        throw new NoSuchElementException("Cannot find attribute with name: " + name + "!");
    }
}
