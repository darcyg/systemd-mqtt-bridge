package org.freedesktop.systemd1;
import org.freedesktop.dbus.Position;
import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.Variant;
public final class Struct2 extends Struct
{
   @Position(0)
   public final String a;
   @Position(1)
   public final Variant b;
  public Struct2(String a, Variant b)
  {
   this.a = a;
   this.b = b;
  }
}
