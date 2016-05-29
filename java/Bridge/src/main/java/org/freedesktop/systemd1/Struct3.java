package org.freedesktop.systemd1;
import java.util.List;
import org.freedesktop.dbus.Position;
import org.freedesktop.dbus.Struct;
public final class Struct3 extends Struct
{
   @Position(0)
   public final String a;
   @Position(1)
   public final List<Struct19> b;
  public Struct3(String a, List<Struct19> b)
  {
   this.a = a;
   this.b = b;
  }
}
