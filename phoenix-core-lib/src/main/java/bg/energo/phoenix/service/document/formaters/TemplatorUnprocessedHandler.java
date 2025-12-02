package bg.energo.phoenix.service.document.formaters;

import hr.ngs.templater.DocumentFactoryBuilder;
import hr.ngs.templater.Templater;

public class TemplatorUnprocessedHandler implements DocumentFactoryBuilder.UnprocessedTagsHandler {

    @Override
    public void onUnprocessed(String s, Templater templater, String[] tags, Object o) {
        for (String tag : tags) {
            templater.replace(tag, "");
        }
    }
}
