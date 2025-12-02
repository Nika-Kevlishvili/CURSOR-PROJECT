package bg.energo.phoenix.model.documentModels;

public class ReschedulingManagerModel {
    public String Title;
    public String Name;
    public String Surname;
    public String JobPosition;

    public ReschedulingManagerModel(
            String title,
            String name,
            String surname,
            String jobPosition
    ) {
        this.Title = title;
        this.Name = name;
        this.Surname = surname;
        this.JobPosition = jobPosition;
    }
}
