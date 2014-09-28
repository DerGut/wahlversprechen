package models

import java.io.FileReader

import models.Rating._
import java.util.Date
import eu.henkelmann.actuarius.{Transformer, Decorator}

import play.api.templates.Html
import play.api.Play
import play.api.Play.current
import play.api.Logger

object Formatter {

	def glyph(rating: Rating) : String = {
		rating match {
			case PromiseKept => "glyphicon-thumbs-up"
			case Compromise => "glyphicon-adjust"
			case PromiseBroken => "glyphicon-thumbs-down"
			case Stalled => "glyphicon-time"
			case InTheWorks => "glyphicon-cog"
			case Unrated => "glyphicon-question-sign"
		}
	}

	def color(rating: Rating) : String = {
		rating match {
			case PromiseKept => "#5cb85c"
			case Compromise => "#f0ad4e"
			case PromiseBroken => "#d9534f"
			case Stalled => "#d9984f"
			case InTheWorks => "#5bc0de"
			case Unrated => "#aaaaaa"
		}
	}

	def name(rating: Rating) : String = {
		rating match {
			case PromiseKept => "Gehalten"
			case Compromise => "Kompromiss"
			case PromiseBroken => "Gebrochen"
			case Stalled => "Blockiert"
			case InTheWorks => "In Arbeit"
			case Unrated => "Unbewertet"
		}
	}

	def icon(rating: Rating)(implicit request: play.api.mvc.RequestHeader) : String = {
		val file = rating match {
			case PromiseKept => "kept"
			case Compromise => "compromise"
			case PromiseBroken => "broken"
			case Stalled => "stalled"
			case InTheWorks => "intheworks"
			case Unrated => "unrated"
		}
		controllers.routes.Assets.at("img/ratings/" + file + ".png").absoluteURL(false)
	}

	def url : String = Play.configuration.getString("application.root_url").get
	def twitter : String = Play.configuration.getString("application.twitter").get
 	def mail : String = Play.configuration.getString("application.mail").get
	def disqus_shortname : String = Play.configuration.getString("application.disqus").get

	def FormUrlEncode(str: String) = java.net.URLEncoder.encode(str, "UTF-8")
	def MIMEEncode(str: String) = play.utils.UriEncoding.encodePathSegment(str, "UTF-8")

	def format(date: Date)(implicit lang: play.api.i18n.Lang) : String = {
		new java.text.SimpleDateFormat("dd.MM.yy", lang.toLocale).format(date)
	}

	val RFC822DateFormatter = new java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", java.util.Locale.US)
	def formatRFC822(date: Date) : String = {
		RFC822DateFormatter.format(date);
	}

	val ISO8601DateFormatter = {
		val df = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
	    df.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
	    df
	}

	def formatISO8601(date: Date) : String = {
		ISO8601DateFormatter.format(date);
	}

	val DayDateFormatter = {
		val df = new java.text.SimpleDateFormat("yyyyMMdd");
		df.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
		df
	}

	def mustache(context: Map[String, Object], template: String) : Html = {
		import com.github.mustachejava._
		import scala.collection.JavaConversions._

	  	var reader : FileReader = null
	  	try {
			reader = new FileReader("public/template/"+template)

			val mustache = new DefaultMustacheFactory().compile(reader, template)

			val writer = new java.io.StringWriter()
			mustache.execute(writer, new java.util.HashMap[String, Object](context) )
			Html( writer.toString() )
		} catch {
			case e: Exception =>
				Logger.error("Error executing mustache template " + template, e)
			  	Html("")
		} finally {
			if(reader!=null) reader.close()
		}
	}

	def favicon(strUrl: String) : String = {
		val url = new java.net.URL(strUrl)
	  	"//www.google.com/s2/favicons?domain=" + url.getHost
	}

	def socialMetaTags(url: String, description: String, img: String) : Html = {
		// Twitter falls back to open graph tags: https://dev.twitter.com/docs/cards/getting-started#open-graph
		Html(
			s"""<meta property="og:image" content="$img">
		    <meta property="og:url" content="$url">
		    <meta property="og:description" content="$description">"""
		)
	}

	private object FilterHeadlineFromMarkdown extends Decorator {
	    override def allowVerbatimXml():Boolean = false
	    override def decorateImg(alt:String, src:String, title:Option[String]):String = ""
	    override def decorateRuler():String = ""
	    override def decorateHeaderOpen(headerNo:Int):String = "<div style='display: none'>"
	    override def decorateHeaderClose(headerNo:Int):String = "</div>"
	    override def decorateCodeBlockOpen():String = "<div 'display: none'>"
	    override def decorateCodeBlockClose():String = "<div 'display: none'>"
	}

	private object FilterXMLFromMarkdown extends Decorator {
	    override def allowVerbatimXml() : Boolean =
	    	Play.configuration.getBoolean("application.allowHTMLEntries").getOrElse(false)
	}

	private object FilterPlainTextFromMarkdown extends Decorator {
   		override def allowVerbatimXml():Boolean = false
		override def decorateBreak():String = "\n"
   	   	override def decorateCode(code:String):String = code
    	override def decorateEmphasis(text:String):String = text
    	override def decorateStrong(text:String):String = text
    	override def decorateLink(text:String, url:String, title:Option[String]):String = text
    	override def decorateImg(alt:String, src:String, title:Option[String]):String = ""
    	override def decorateRuler():String = "\n\n"
    	override def decorateHeaderOpen(headerNo:Int):String = ""
    	override def decorateHeaderClose(headerNo:Int):String = "\n"
    	override def decorateCodeBlockOpen():String = ""
    	override def decorateCodeBlockClose():String = "\n"
        override def decorateParagraphOpen():String = ""
		override def decorateParagraphClose():String = "\n\n"
		override def decorateBlockQuoteOpen():String = ""
		override def decorateBlockQuoteClose():String = "\n"
		override def decorateItemOpen():String = ""
		override def decorateItemClose():String = "\n"
		override def decorateUListOpen():String = ""
		override def decorateUListClose():String = "\n"
		override def decorateOListOpen():String = ""
		override def decorateOListClose():String = "\n"
	}

	private object markdownToHTMLWithoutHeadlines extends Transformer {
		 override def deco() : Decorator = FilterHeadlineFromMarkdown
	}
	def transformBodyToHTML(markdown: String) : Html = {
		Html(markdownToHTMLWithoutHeadlines(markdown))
	}

	private object markdownToHTML extends Transformer {
		 override def deco() : Decorator = FilterXMLFromMarkdown
	}
	def transformToHTML(markdown: String) : Html = {
		Html(markdownToHTML(markdown))
	}

	private object markdownToText extends Transformer {
		 override def deco() : Decorator = FilterPlainTextFromMarkdown
	}
	def transformToText(markdown: String) : String = {
		markdownToText(markdown)
	}
	
	
}
