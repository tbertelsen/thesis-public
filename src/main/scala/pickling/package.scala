// import scala.pickling.json.JSONPickleFormat

package object pickling {
  def compactJson(string: String) = {
    string.replaceAll("[\r\n]+ *", " ")
  }
}
