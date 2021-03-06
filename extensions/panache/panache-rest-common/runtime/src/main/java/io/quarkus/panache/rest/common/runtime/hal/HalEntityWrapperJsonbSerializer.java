package io.quarkus.panache.rest.common.runtime.hal;

import java.util.Map;

import javax.json.bind.serializer.JsonbSerializer;
import javax.json.bind.serializer.SerializationContext;
import javax.json.stream.JsonGenerator;

import org.eclipse.yasson.internal.Marshaller;
import org.eclipse.yasson.internal.model.ClassModel;
import org.eclipse.yasson.internal.model.PropertyModel;

public class HalEntityWrapperJsonbSerializer implements JsonbSerializer<HalEntityWrapper> {

    private final HalLinksExtractor linksExtractor;

    public HalEntityWrapperJsonbSerializer() {
        this.linksExtractor = new RestEasyHalLinksExtractor();
    }

    HalEntityWrapperJsonbSerializer(HalLinksExtractor linksExtractor) {
        this.linksExtractor = linksExtractor;
    }

    @Override
    public void serialize(HalEntityWrapper wrapper, JsonGenerator generator, SerializationContext context) {
        Marshaller marshaller = (Marshaller) context;
        Object entity = wrapper.getEntity();

        if (!marshaller.addProcessedObject(entity)) {
            throw new RuntimeException("Cyclic dependency when marshaling an object");
        }

        try {
            generator.writeStartObject();
            ClassModel classModel = marshaller.getMappingContext().getOrCreateClassModel(entity.getClass());

            for (PropertyModel property : classModel.getSortedProperties()) {
                if (property.isReadable()) {
                    context.serialize(property.getWriteName(), property.getValue(entity), generator);
                }
            }

            writeLinks(entity, generator, context);
            generator.writeEnd();
        } finally {
            marshaller.removeProcessedObject(entity);
        }
    }

    private void writeLinks(Object entity, JsonGenerator generator, SerializationContext context) {
        Map<String, HalLink> links = linksExtractor.getLinks(entity);
        context.serialize("_links", links, generator);
    }
}
