import sbt._

sealed abstract class TypedResourceType{
  protected val base:String
  protected def resourceLine(id:String,classname:String):String
  protected def layout(layoutName:String):String

  def generateFiles(path:File,packageName:String,resources:Map[String,String],layouts:Seq[Option[String]]):Seq[File]

  /*
 }
  */
}

object TypedResourceType{


  object Java extends TypedResourceType{
    val typedResource = 
   """|package %s;
      |class TypedResource<T>{
      |  public final int id;
      |  public TypedResource(final int id){
      |    this.id = id;
      |  }
      |}"""

    val typedLayout = 
   """|package %s;
      |class TypedLayout{
      |  public final int id;
      |  public TypedLayout(final int id){
      |    this.id = id;
      |  }
      |}"""
      
    val tr =
   """|package %s;
      |import android.app.Activity;
      |import android.app.Dialog;
      |import android.view.View;
      |
      |public class TR {
      |%s
      | public static class layout {
      | %s
      | }
      |}"""

    val typedViewHolder =
   """|abstract class TypedViewHolder {
      |  abstract public View findViewById(int id);
      |  public final <T> T findView(TypedResource<T> tr){
      |    return (T)findViewById(tr.id);
      |  }
      |
      |  public static TypedViewHolder getInstance(final View v){
      |    return new TypedViewHolder(){ 
      |      public View findViewById(int id){
      |        return v.findViewById(id);
      |      }
      |    };
      |  }
      |
      |  public static TypedViewHolder getInstance(final Activity v){
      |    return new TypedViewHolder(){ 
      |      public View findViewById(int id){
      |        return v.findViewById(id);
      |      }
      |    };
      |  }
      |
      |  public static TypedViewHolder getInstance(final Dialog v){
      |    return new TypedViewHolder(){ 
      |      public View findViewById(int id){
      |        return v.findViewById(id);
      |      }
      |    };
      |  }
      |}
      |"""

    def resourceLine(id:String,classname:String) = 
      "  public static TypedResource<%s> %s = new TypedResource<%s>(R.id.%s);".format(classname, id, classname, id)

    def layout(layoutName:String) =
      "   public static TypedLayout %s = new TypedLayout(R.layout.%s);".format(layoutName, layoutName)
  }
  

  object Scala extends TypedResourceType{
    def generateFiles(path:String,packageName:String,resources:Map[String,String],layouts:Seq[Option[String]]):Seq[File] = 
      IO.write(
        path ,
        base.format(
          packageName,
          resources.map(Function tupled resourceLine) mkString "\n",
          layouts map {
            _.map(layout) getOrElse ""
          } mkString "\n"
        ) 
      ) 
 
    val fileName = "TR.scala"
    val base =
   """|package %s
      |import _root_.android.app.{Activity, Dialog}
      |import _root_.android.view.View
      |
      |case class TypedResource[T](id: Int)
      |case class TypedLayout(id: Int)
      |
      |object TR {
      |%s
      | object layout {
      | %s
      | }
      |}
      |trait TypedViewHolder {
      |  def findViewById( id: Int ): View
      |  def findView[T](tr: TypedResource[T]) = findViewById(tr.id).asInstanceOf[T]
      |}
      |trait TypedView extends View with TypedViewHolder
      |trait TypedActivityHolder extends TypedViewHolder
      |trait TypedActivity extends Activity with TypedActivityHolder
      |trait TypedDialog extends Dialog with TypedViewHolder
      |object TypedResource {
      |  implicit def layout2int(l: TypedLayout) = l.id
      |  implicit def view2typed(v: View) = new TypedViewHolder { 
      |    def findViewById( id: Int ) = v.findViewById( id )
      |  }
      |  implicit def activity2typed(a: Activity) = new TypedViewHolder { 
      |    def findViewById( id: Int ) = a.findViewById( id )
      |  }
      |  implicit def dialog2typed(d: Dialog) = new TypedViewHolder { 
      |    def findViewById( id: Int ) = d.findViewById( id )
      |  }
      |}
      |""".stripMargin

    def resourceLine(id:String,classname:String) = 
      "  val %s = TypedResource[%s](R.id.%s)".format(id, classname, id)

    def layout(layoutName:String) =
      " val %s = TypedLayout(R.layout.%s)".format(layoutName, layoutName)
  }
  
}
