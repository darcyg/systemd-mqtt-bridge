package org.freedesktop.systemd1;
import org.freedesktop.dbus.Position;
import org.freedesktop.dbus.Struct;
public final class Struct9 extends Struct
{
   @Position(0)
   public final String a;
   @Position(1)
   public final String b;
   @Position(2)
   public final String c;
  public Struct9(String a, String b, String c)
  {
   this.a = a;
   this.b = b;
   this.c = c;
  }
}
