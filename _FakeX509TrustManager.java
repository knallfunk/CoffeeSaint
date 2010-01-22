import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;

public class _FakeX509TrustManager implements javax.net.ssl.X509TrustManager {
	private static final X509Certificate[] _AcceptedIssuers =
		new X509Certificate[] {};

	public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
		}

	public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
		}

	public boolean isClientTrusted(X509Certificate[] chain) {
		return(true);
	}

	public boolean isServerTrusted(X509Certificate[] chain) {
		return(true);
	}

	public X509Certificate[] getAcceptedIssuers() {
		return(_AcceptedIssuers);
	}
}
