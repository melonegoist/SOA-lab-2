package org.itmo.shop.infrastructure.web;

import org.itmo.shop.infrastructure.web.generated.model.VehicleTypeDto;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.util.function.Function;

@ControllerAdvice
class StrictParameterBinding {

    @InitBinder
    void strictConversions(WebDataBinder binder) {
        binder.registerCustomEditor(VehicleTypeDto.class, strict(VehicleTypeDto::fromValue));
        binder.registerCustomEditor(Integer.class, strict(Integer::valueOf));
    }

    private static PropertyEditor strict(Function<String, ?> parser) {
        return new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(parser.apply(text));
            }
        };
    }
}
