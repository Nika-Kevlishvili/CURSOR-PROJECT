package bg.energo.phoenix.util.document;

import bg.energo.phoenix.model.entity.documents.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DocumentSignerWrapper<T> extends ArrayList<T> {

    private final Document document;
    private final List<T> list;

    public DocumentSignerWrapper(List<T> list, Document document) {
        super(list);
        this.document=document;
        this.list=list;
    }

    @Override
    public T remove(int index) {
        this.document.setStatusModifyDate(LocalDateTime.now());
        this.list.remove(index);
        return super.remove(index);
    }

    @Override
    public boolean add(T t) {
        this.document.setStatusModifyDate(LocalDateTime.now());
        this.list.add(t);
        return super.add(t);
    }

    @Override
    public boolean remove(Object o) {
        this.document.setStatusModifyDate(LocalDateTime.now());
        this.list.remove(o);
        return super.remove(o);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        this.document.setStatusModifyDate(LocalDateTime.now());
        this.list.addAll(c);
        return super.addAll(c);
    }



}
