package com.toraysoft.wechatpay;

import java.util.List;

import org.apache.http.NameValuePair;

import android.content.Context;
import android.util.Log;

import com.tencent.mm.sdk.constants.Build;
import com.tencent.mm.sdk.modelpay.PayReq;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

public class WechatPayHelper {

	static WechatPayHelper instance;

	private IWXAPI api;

	Context context;
//	private static final String APP_KEY = "qW4DTKIPVk8vZwoUQWvcxurnnZfEkF495dn0jiDfRrAuyeA41XZ2CIO4zScI4L3mGLcSTwWzjiaHrPMMhu8GlVkZ7oJGWDb79KTBhdj0bHk7vKVCNZYKIzZonzAL7p7F";
	
	public static WechatPayHelper get(Context context) {
		if (instance == null) {
			instance = new WechatPayHelper();
			instance.context = context;
		}
		return instance;
	}

	IWXAPI getApi(String app_id) throws WechatNotSupportException {
		if (api == null) {
			api = WXAPIFactory.createWXAPI(context, app_id);
			api.registerApp(app_id);
		}

		if (api.getWXAppSupportAPI() < Build.PAY_SUPPORTED_SDK_INT) {
			throw new WechatNotSupportException();
		}
		return api;
	}

	public void pay(String app_id, String partner_id, String prepay_id,
			String nonceStr, String timeStamp, String packageValue, String sign)
			throws WechatNotSupportException {

		PayReq req = new PayReq();
		req.appId = app_id;
		req.partnerId = partner_id;
		req.prepayId = prepay_id;
		req.nonceStr = nonceStr;
		req.timeStamp = timeStamp;
		req.packageValue = packageValue;
		
//		List<NameValuePair> signParams = new LinkedList<NameValuePair>();
//		signParams.add(new BasicNameValuePair("appid", req.appId));
//		signParams.add(new BasicNameValuePair("appkey", APP_KEY));
//		signParams.add(new BasicNameValuePair("noncestr", req.nonceStr));
//		signParams.add(new BasicNameValuePair("package", req.packageValue));
//		signParams.add(new BasicNameValuePair("partnerid", req.partnerId));
//		signParams.add(new BasicNameValuePair("prepayid", req.prepayId));
//		signParams.add(new BasicNameValuePair("timestamp", req.timeStamp));
//		req.sign = genSign(signParams);

		req.sign = sign;
		getApi(app_id).sendReq(req);
	}

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

		Log.d("test", "package = "+sb.toString());
		String sha1 = Util.sha1(sb.toString());
		Log.d("test", "genSign, sha1 = " + sha1);
		return sha1;
	}

}
