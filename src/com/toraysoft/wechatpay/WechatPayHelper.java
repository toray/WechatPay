package com.toraysoft.wechatpay;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.tencent.mm.sdk.modelpay.PayReq;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

public class WechatPayHelper {
	
	private static final String TAG = "WechatPay";

	static WechatPayHelper instance; 
	
	private IWXAPI api;
	
	// APP_ID �滻Ϊ���Ӧ�ôӹٷ���վ���뵽�ĺϷ�appId
    public static final String APP_ID = "wxd930ea5d5a258f4f";
    
    /** �̼���Ƹ�ͨ������̼�id */
    public static final String PARTNER_ID = "1900000109";

	/**
	 * ΢�Ź���ƽ̨�̻�ģ����̻�Լ������Կ
	 * 
	 * ע�⣺����hardcode�ڿͻ��ˣ�����genPackage��������ɷ����������
	 */
	private static final String PARTNER_KEY = "8934e7d15453e97507ef794cf7b0519d";

	 /**
   * ΢�ſ���ƽ̨���̻�Լ������Կ
   * 
   * ע�⣺����hardcode�ڿͻ��ˣ�����genSign��������ɷ����������
   */
	private static final String APP_SECRET = "db426a9829e4b49a0dcac7b4162da6b6"; // wxd930ea5d5a258f4f ��Ӧ����Կ
	
	/**
   * ΢�ſ���ƽ̨���̻�Լ����֧����Կ
   * 
   * ע�⣺����hardcode�ڿͻ��ˣ�����genSign��������ɷ����������
   */
	private static final String APP_KEY = "L8LrMqqeGRxST5reouB0K66CaYAWpqhAVsq7ggKkxHCOastWksvuX1uvmvQclxaHoYd3ElNBrNO2DHnnzgfVG9Qs473M3DTOZug5er46FhuGofumV8H2FVR9qkjSlC5K"; // wxd930ea5d5a258f4f ��Ӧ��֧����Կ
	
	
	public static WechatPayHelper get(Context context){
		if(instance == null){
			instance = new WechatPayHelper();
			instance.api = WXAPIFactory.createWXAPI(context, APP_ID);
		}
		return instance;
	}
	
	private String genPackage(List<NameValuePair> params) {
		StringBuilder sb = new StringBuilder();
		
		for (int i = 0; i < params.size(); i++) {
			sb.append(params.get(i).getName());
			sb.append('=');
			sb.append(params.get(i).getValue());
			sb.append('&');
		}
		sb.append("key=");
		sb.append(PARTNER_KEY); // ע�⣺����hardcode�ڿͻ��ˣ�����genPackage������̶��ɷ����������
		
		// ����md5ժҪǰ��params����Ϊԭʼ���ݣ�δ����url encode����
		String packageSign = MD5.getMessageDigest(sb.toString().getBytes()).toUpperCase();
		
		return URLEncodedUtils.format(params, "utf-8") + "&sign=" + packageSign;
	}
	
	private class GetAccessTokenTask extends AsyncTask<Void, Void, GetAccessTokenResult> {

		@Override
		protected void onPostExecute(GetAccessTokenResult result) {
			
			if (result.localRetCode == LocalRetCode.ERR_OK) {
//				Toast.makeText(PayActivity.this, R.string.get_access_token_succ, Toast.LENGTH_LONG).show();
				Log.d(TAG, "onPostExecute, accessToken = " + result.accessToken);
				
				GetPrepayIdTask getPrepayId = new GetPrepayIdTask(result.accessToken);
				getPrepayId.execute();
			} else {
//				Toast.makeText(PayActivity.this, getString(R.string.get_access_token_fail, result.localRetCode.name()), Toast.LENGTH_LONG).show();
			}
		}

		@Override
		protected GetAccessTokenResult doInBackground(Void... params) {
			GetAccessTokenResult result = new GetAccessTokenResult();

			String url = String.format("https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s",
					APP_ID, APP_SECRET);
			Log.d(TAG, "get access token, url = " + url);
			
			byte[] buf = Util.httpGet(url);
			if (buf == null || buf.length == 0) {
				result.localRetCode = LocalRetCode.ERR_HTTP;
				return result;
			}
			
			String content = new String(buf);
			result.parseFrom(content);
			return result;
		}
	}
	
	private class GetPrepayIdTask extends AsyncTask<Void, Void, GetPrepayIdResult> {

		private String accessToken;
		
		public GetPrepayIdTask(String accessToken) {
			this.accessToken = accessToken;
		}
		
		@Override
		protected void onPostExecute(GetPrepayIdResult result) {
			if (result.localRetCode == LocalRetCode.ERR_OK) {
//				Toast.makeText(PayActivity.this, R.string.get_prepayid_succ, Toast.LENGTH_LONG).show();
				sendPayReq(result);
			} else {
//				Toast.makeText(PayActivity.this, getString(R.string.get_prepayid_fail, result.localRetCode.name()), Toast.LENGTH_LONG).show();
			}
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
		}

		@Override
		protected GetPrepayIdResult doInBackground(Void... params) {

			String url = String.format("https://api.weixin.qq.com/pay/genprepay?access_token=%s", accessToken);
			String entity = genProductArgs();
			
			Log.d(TAG, "doInBackground, url = " + url);
			Log.d(TAG, "doInBackground, entity = " + entity);
			
			GetPrepayIdResult result = new GetPrepayIdResult();
			
			byte[] buf = Util.httpPost(url, entity);
			if (buf == null || buf.length == 0) {
				result.localRetCode = LocalRetCode.ERR_HTTP;
				return result;
			}
			
			String content = new String(buf);
			Log.d(TAG, "doInBackground, content = " + content);
			result.parseFrom(content);
			return result;
		}
	}

	private static enum LocalRetCode {
		ERR_OK, ERR_HTTP, ERR_JSON, ERR_OTHER
	}
	
	private static class GetAccessTokenResult {
		
		private static final String TAG = "MicroMsg.SDKSample.PayActivity.GetAccessTokenResult";
		
		public LocalRetCode localRetCode = LocalRetCode.ERR_OTHER;
		public String accessToken;
		public int expiresIn;
		public int errCode;
		public String errMsg;
		
		public void parseFrom(String content) {

			if (content == null || content.length() <= 0) {
				Log.e(TAG, "parseFrom fail, content is null");
				localRetCode = LocalRetCode.ERR_JSON;
				return;
			}
			
			try {
				JSONObject json = new JSONObject(content);
				if (json.has("access_token")) { // success case
					accessToken = json.getString("access_token");
					expiresIn = json.getInt("expires_in");
					localRetCode = LocalRetCode.ERR_OK;
				} else {
					errCode = json.getInt("errcode");
					errMsg = json.getString("errmsg");
					localRetCode = LocalRetCode.ERR_JSON;
				}
				
			} catch (Exception e) {
				localRetCode = LocalRetCode.ERR_JSON;
			}
		}
	}
	
	private static class GetPrepayIdResult {
		
		private static final String TAG = "MicroMsg.SDKSample.PayActivity.GetPrepayIdResult";
		
		public LocalRetCode localRetCode = LocalRetCode.ERR_OTHER;
		public String prepayId;
		public int errCode;
		public String errMsg;
		
		public void parseFrom(String content) {
			
			if (content == null || content.length() <= 0) {
				Log.e(TAG, "parseFrom fail, content is null");
				localRetCode = LocalRetCode.ERR_JSON;
				return;
			}
			
			try {
				JSONObject json = new JSONObject(content);
				if (json.has("prepayid")) { // success case
					prepayId = json.getString("prepayid");
					localRetCode = LocalRetCode.ERR_OK;
				} else {
					localRetCode = LocalRetCode.ERR_JSON;
				}
				
				errCode = json.getInt("errcode");
				errMsg = json.getString("errmsg");
				
			} catch (Exception e) {
				localRetCode = LocalRetCode.ERR_JSON;
			}
		}
	}
	
	private String genNonceStr() {
		Random random = new Random();
		return MD5.getMessageDigest(String.valueOf(random.nextInt(10000)).getBytes());
	}
	
	private long genTimeStamp() {
		return System.currentTimeMillis() / 1000;
	}
	
	/**
	 * ���� traceid �ֶΰ����û���Ϣ��������Ϣ����������Զ���״̬�Ĳ�ѯ�͸���
	 */
	private String getTraceId() {
		return "crestxu_" + genTimeStamp(); 
	}
	
	/**
	 * ע�⣺�̻�ϵͳ�ڲ��Ķ�����,32���ַ��ڡ��ɰ�����ĸ,ȷ�����̻�ϵͳΨһ
	 */
	private String genOutTradNo() {
		Random random = new Random();
		return MD5.getMessageDigest(String.valueOf(random.nextInt(10000)).getBytes());
	}
	
	private long timeStamp;
	private String nonceStr, packageValue; 
	
	private String genSign(List<NameValuePair> params) {
		StringBuilder sb = new StringBuilder();
		
		int i = 0;
		for (; i < params.size() - 1; i++) {
			sb.append(params.get(i).getName());
			sb.append('=');
			sb.append(params.get(i).getValue());
			sb.append('&');
		}
		sb.append(params.get(i).getName());
		sb.append('=');
		sb.append(params.get(i).getValue());
		
		String sha1 = Util.sha1(sb.toString());
		Log.d(TAG, "genSign, sha1 = " + sha1);
		return sha1;
	}
	
	private String genProductArgs() {
		JSONObject json = new JSONObject();
		
		try {
			json.put("appid", APP_ID);
			String traceId = getTraceId();  // traceId �ɿ������Զ��壬�����ڶ����Ĳ�ѯ����٣��������֧���û���Ϣ���ɴ�id
			json.put("traceid", traceId);
			nonceStr = genNonceStr();
			json.put("noncestr", nonceStr);
			
			List<NameValuePair> packageParams = new LinkedList<NameValuePair>();
			packageParams.add(new BasicNameValuePair("bank_type", "WX"));
			packageParams.add(new BasicNameValuePair("body", "ǧ��𹿰�"));
			packageParams.add(new BasicNameValuePair("fee_type", "1"));
			packageParams.add(new BasicNameValuePair("input_charset", "UTF-8"));
			packageParams.add(new BasicNameValuePair("notify_url", "http://weixin.qq.com"));
			packageParams.add(new BasicNameValuePair("out_trade_no", genOutTradNo()));
			packageParams.add(new BasicNameValuePair("partner", "1900000109"));
			packageParams.add(new BasicNameValuePair("spbill_create_ip", "196.168.1.1"));
			packageParams.add(new BasicNameValuePair("total_fee", "1"));
			packageValue = genPackage(packageParams);
			
			json.put("package", packageValue);
			timeStamp = genTimeStamp();
			json.put("timestamp", timeStamp);
			
			List<NameValuePair> signParams = new LinkedList<NameValuePair>();
			signParams.add(new BasicNameValuePair("appid", APP_ID));
			signParams.add(new BasicNameValuePair("appkey", APP_KEY));
			signParams.add(new BasicNameValuePair("noncestr", nonceStr));
			signParams.add(new BasicNameValuePair("package", packageValue));
			signParams.add(new BasicNameValuePair("timestamp", String.valueOf(timeStamp)));
			signParams.add(new BasicNameValuePair("traceid", traceId));
			json.put("app_signature", genSign(signParams));
			
			json.put("sign_method", "sha1");
		} catch (Exception e) {
			Log.e(TAG, "genProductArgs fail, ex = " + e.getMessage());
			return null;
		}
		
		return json.toString();
	}
	
	private void sendPayReq(GetPrepayIdResult result) {
		
		PayReq req = new PayReq();
		req.appId = APP_ID;
		req.partnerId = PARTNER_ID;
		req.prepayId = result.prepayId;
		req.nonceStr = nonceStr;
		req.timeStamp = String.valueOf(timeStamp);
		req.packageValue = "Sign=" + packageValue;
		
		List<NameValuePair> signParams = new LinkedList<NameValuePair>();
		signParams.add(new BasicNameValuePair("appid", req.appId));
		signParams.add(new BasicNameValuePair("appkey", APP_KEY));
		signParams.add(new BasicNameValuePair("noncestr", req.nonceStr));
		signParams.add(new BasicNameValuePair("package", req.packageValue));
		signParams.add(new BasicNameValuePair("partnerid", req.partnerId));
		signParams.add(new BasicNameValuePair("prepayid", req.prepayId));
		signParams.add(new BasicNameValuePair("timestamp", req.timeStamp));
		req.sign = genSign(signParams);
		
		// ��֧��֮ǰ�����Ӧ��û��ע�ᵽ΢�ţ�Ӧ���ȵ���IWXMsg.registerApp��Ӧ��ע�ᵽ΢��
		api.sendReq(req);
	}
}
