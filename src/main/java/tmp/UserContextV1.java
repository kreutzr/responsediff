package tmp;

public class UserContextV1 implements IUserContext{
  private String version;
  private String thema;

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getThema() {
    return thema;
  }

  public void setThema(String thema) {
    this.thema = thema;
  }
}
