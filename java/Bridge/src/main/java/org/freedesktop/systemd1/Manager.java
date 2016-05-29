package org.freedesktop.systemd1;
import java.util.List;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.UInt32;
import org.freedesktop.dbus.UInt64;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.systemd1.Privileged;
public interface Manager extends DBusInterface
{
   public static class UnitNew extends DBusSignal
   {
      public final String a;
      public final DBusInterface b;
      public UnitNew(String path, String a, DBusInterface b) throws DBusException
      {
         super(path, a, b);
         this.a = a;
         this.b = b;
      }
   }
   public static class UnitRemoved extends DBusSignal
   {
      public final String a;
      public final DBusInterface b;
      public UnitRemoved(String path, String a, DBusInterface b) throws DBusException
      {
         super(path, a, b);
         this.a = a;
         this.b = b;
      }
   }
   public static class JobNew extends DBusSignal
   {
      public final UInt32 a;
      public final DBusInterface b;
      public final String c;
      public JobNew(String path, UInt32 a, DBusInterface b, String c) throws DBusException
      {
         super(path, a, b, c);
         this.a = a;
         this.b = b;
         this.c = c;
      }
   }
   public static class JobRemoved extends DBusSignal
   {
      public final UInt32 a;
      public final DBusInterface b;
      public final String c;
      public final String d;
      public JobRemoved(String path, UInt32 a, DBusInterface b, String c, String d) throws DBusException
      {
         super(path, a, b, c, d);
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
      }
   }
   public static class StartupFinished extends DBusSignal
   {
      public final UInt64 a;
      public final UInt64 b;
      public final UInt64 c;
      public final UInt64 d;
      public final UInt64 e;
      public final UInt64 f;
      public StartupFinished(String path, UInt64 a, UInt64 b, UInt64 c, UInt64 d, UInt64 e, UInt64 f) throws DBusException
      {
         super(path, a, b, c, d, e, f);
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
      }
   }
   public static class UnitFilesChanged extends DBusSignal
   {
      public UnitFilesChanged(String path) throws DBusException
      {
         super(path);
      }
   }
   public static class Reloading extends DBusSignal
   {
      public final boolean a;
      public Reloading(String path, boolean a) throws DBusException
      {
         super(path, a);
         this.a = a;
      }
   }

  public DBusInterface GetUnit(String a);
  public DBusInterface GetUnitByPID(UInt32 a);
  public DBusInterface LoadUnit(String a);
  public DBusInterface StartUnit(String a, String b);
  public DBusInterface StartUnitReplace(String a, String b, String c);
  public DBusInterface StopUnit(String a, String b);
  public DBusInterface ReloadUnit(String a, String b);
  public DBusInterface RestartUnit(String a, String b);
  public DBusInterface TryRestartUnit(String a, String b);
  public DBusInterface ReloadOrRestartUnit(String a, String b);
  public DBusInterface ReloadOrTryRestartUnit(String a, String b);
  public void KillUnit(String a, String b, int c);
  public void ResetFailedUnit(String a);
  public void SetUnitProperties(String a, boolean b, List<Struct1> c);
  public DBusInterface StartTransientUnit(String a, String b, List<Struct2> c, List<Struct3> d);
  public DBusInterface GetJob(UInt32 a);
  public void CancelJob(UInt32 a);
  public void ClearJobs();
  public void ResetFailed();
  public List<Struct4> ListUnits();
  public List<Struct5> ListUnitsFiltered(List<String> a);
  public List<Struct6> ListJobs();
  public void Subscribe();
  public void Unsubscribe();
  public String Dump();
  public DBusInterface CreateSnapshot(String a, boolean b);
  public void RemoveSnapshot(String a);
  public void Reload();
  public void Reexecute();
  @Privileged("true")
  public void Exit();
  @Privileged("true")
  public void Reboot();
  @Privileged("true")
  public void PowerOff();
  @Privileged("true")
  public void Halt();
  @Privileged("true")
  public void KExec();
  @Privileged("true")
  public void SwitchRoot(String a, String b);
  public void SetEnvironment(List<String> a);
  public void UnsetEnvironment(List<String> a);
  public void UnsetAndSetEnvironment(List<String> a, List<String> b);
  public List<Struct7> ListUnitFiles();
  public String GetUnitFileState(String a);
  public Pair<Boolean, List<Struct8>> EnableUnitFiles(List<String> a, boolean b, boolean c);
  public List<Struct9> DisableUnitFiles(List<String> a, boolean b);
  public Pair<Boolean, List<Struct10>> ReenableUnitFiles(List<String> a, boolean b, boolean c);
  public List<Struct11> LinkUnitFiles(List<String> a, boolean b, boolean c);
  public Pair<Boolean, List<Struct12>> PresetUnitFiles(List<String> a, boolean b, boolean c);
  public Pair<Boolean, List<Struct13>> PresetUnitFilesWithMode(List<String> a, String b, boolean c, boolean d);
  public List<Struct14> MaskUnitFiles(List<String> a, boolean b, boolean c);
  public List<Struct15> UnmaskUnitFiles(List<String> a, boolean b);
  public List<Struct16> SetDefaultTarget(String a, boolean b);
  public String GetDefaultTarget();
  public List<Struct17> PresetAllUnitFiles(String a, boolean b, boolean c);
  public List<Struct18> AddDependencyUnitFiles(List<String> a, String b, String c, boolean d, boolean e);

}
