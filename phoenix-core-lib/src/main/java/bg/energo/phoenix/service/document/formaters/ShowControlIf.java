package bg.energo.phoenix.service.document.formaters;

import hr.ngs.templater.DocumentFactoryBuilder;
import hr.ngs.templater.Handled;

public class ShowControlIf implements DocumentFactoryBuilder.Handler {
    @Override
    public Handled handle(Object value, String metadata, String path, int position,  hr.ngs.templater.Templater templater) {
        if (value != null && metadata.startsWith("showControlIf(")) {
            //Extract the matching expression
            String expression = metadata.substring("showControlIf(".length(), metadata.length() - 1);
            if (String.valueOf(value).equals(expression)) {
                templater.replace(path, "");
                return Handled.THIS_TAG;
            }else{
                if (position == -1)
                    //when position is -1 it means non sharing tag is being used, in which case we can resize that region via "standard" API
                    templater.resize(new String[]{path}, 0);
                else
                    //otherwise we need to use "advanced" resize API to specify which exact tag to replace
                    templater.resize(new hr.ngs.templater.Templater.TagPosition[] { new hr.ngs.templater.Templater.TagPosition(path, position)}, 0);
                return Handled.NESTED_TAGS;
            }
        }
        return Handled.NOTHING;
    }
}
