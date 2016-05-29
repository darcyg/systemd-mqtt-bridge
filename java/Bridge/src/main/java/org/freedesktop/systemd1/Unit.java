package org.freedesktop.systemd1;
import java.util.List;
import org.freedesktop.dbus.DBusInterface;
public interface Unit extends DBusInterface
{

  public DBusInterface Start(String a);
  public DBusInterface Stop(String a);
  public DBusInterface Reload(String a);
  public DBusInterface Restart(String a);
  public DBusInterface TryRestart(String a);
  public DBusInterface ReloadOrRestart(String a);
  public DBusInterface ReloadOrTryRestart(String a);
  public void Kill(String a, int b);
  public void ResetFailed();
  public void SetProperties(boolean a, List<Struct1> b);

}
