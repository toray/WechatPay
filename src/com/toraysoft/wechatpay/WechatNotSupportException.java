package com.toraysoft.wechatpay;

public class WechatNotSupportException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2313964703740528072L;

	public WechatNotSupportException(){
		super("Current wechat APP version too low, not support wechat pay.");
	}
}
