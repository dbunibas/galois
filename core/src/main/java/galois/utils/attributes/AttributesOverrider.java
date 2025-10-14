package galois.utils.attributes;

import lombok.extern.slf4j.Slf4j;
import speedy.model.database.Attribute;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
public final class AttributesOverrider {


    public static List<Attribute> overrideAttributes(AttributesOverride override, Set<Attribute> attributes) {
        if (override.getPinnedAttributes() != null) {
            log.warn("Pinned attributes are currently ignored...");
        }

        if (override.getPinInPosition() != null) {
            List<Attribute> result = new ArrayList<>();
            List<Attribute> toPin = new ArrayList<>(attributes);

            for (int i = 0; i < override.getPinInPosition(); i++) {
                result.add(override.getExtraAttributes().get(i));
            }

            result.addAll(toPin);
            result.addAll(override.getExtraAttributes().subList(override.getPinInPosition(), override.getExtraAttributes().size()));
            return result;
        }

        List<Attribute> result = new ArrayList<>(attributes);
        result.addAll(override.getExtraAttributes());
        return result;
    }
}
