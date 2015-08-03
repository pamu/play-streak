package streak

import java.util.Scanner

import org.apache.http.HttpHost
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.impl.auth.BasicScheme
import org.apache.http.impl.client.{BasicAuthCache, HttpClients, BasicCredentialsProvider}

import scala.concurrent.Future

import play.api.libs.concurrent.Execution.Implicits.defaultContext


/**
 * Created by pnagarjuna on 29/06/15.
 */

object Streak {

  case class NoResponseException(msg: String) extends Exception(msg)
  case class NoEntityException(msg: String) extends Exception(msg)

  val TIME_OUT = 20000

  def get(url: String)(key: String) = {
    scala.concurrent.blocking {
      val targetHost = new HttpHost("www.streak.com", 443, "https");
      val httpClient = HttpClients.createDefault()
      val httpGet = new HttpGet(url)

      val requestConfig = RequestConfig.custom()
      .setSocketTimeout(TIME_OUT)
      .setConnectTimeout(TIME_OUT)
      .setConnectionRequestTimeout(TIME_OUT).build()
      httpGet.setConfig(requestConfig);

      val creds = new UsernamePasswordCredentials(key, "")
      val credsProvider = new BasicCredentialsProvider()
      credsProvider.setCredentials(new AuthScope(targetHost.getHostName, targetHost.getPort), creds)
      val basicAuth = new BasicScheme()
      val authCache = new BasicAuthCache()
      authCache.put(targetHost, basicAuth)
      val context = HttpClientContext.create()
      context.setCredentialsProvider(credsProvider)
      context.setAuthCache(authCache)
      val response = httpClient.execute(httpGet, context)
      if (response == null) throw new NoResponseException("No Response")
      val entity = response.getEntity
      if (entity == null) throw new NoEntityException("No Entity")
      val reader = new Scanner(entity.getContent)
      val sb = new StringBuilder()
      while (reader.hasNext) {
        sb.append(reader.nextLine() + "\n")
      }
      val statusCode = response.getStatusLine.getStatusCode
      httpClient.close()
      response.close()
      (statusCode, sb.toString)
    }
  }

  def boxes(implicit key: String) = {
    Future {
      get(URLS.boxes)(key)
    }
  }

  def pipelines(implicit key: String) = {
    Future {
      get(URLS.pipelines)(key)
    }
  }

  def getPipelineBoxes(pipelineKey: String)(implicit key: String) = {
    Future {
      get(URLS.pipelineBoxes(pipelineKey))(key)
    }
  }

  def getPipelineStages(pipelineKey: String)(implicit key: String) = {
    Future {
      get(URLS.pipelineStages(pipelineKey))(key)
    }
  }
  
}
