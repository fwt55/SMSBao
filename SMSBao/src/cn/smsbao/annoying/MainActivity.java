/*
 * 官网地站:http://www.mob.com
 * 技术支持QQ: 4006852216
 * 官方微信:ShareSDK   （如果发布新版本的话，我们将会第一时间通过微信将版本更新内容推送给您。如果使用过程中有任何问题，也可以通过微信与我们取得联系，我们将会在24小时内给予回复）
 *
 * Copyright (c) 2014年 mob.com. All rights reserved.
 */
package cn.smsbao.annoying;

import static com.mob.tools.utils.R.getStringRes;

import java.util.HashMap;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import cn.smssdk.EventHandler;
import cn.smssdk.OnSendMessageHandler;
import cn.smssdk.SMSSDK;
import cn.smssdk.demo.R;
import cn.smssdk.gui.CommonDialog;
import cn.smssdk.gui.ContactsPage;
import cn.smssdk.gui.RegisterPage;

public class MainActivity extends Activity implements OnClickListener, Callback {
	// 填写从短信SDK应用后台注册得到的APPKEY
	private static String APPKEY = "8aa6f5a90110";

	// 填写从短信SDK应用后台注册得到的APPSECRET
	private static String APPSECRET = "7b16e06a9f00f75a29f041956c91cda3";


	private boolean ready;
	private Dialog pd;
	private TextView tvNum;
	private EditText et_phone;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
		Button btnRegist = (Button) findViewById(R.id.btn_bind_phone);
		View btnContact = findViewById(R.id.rl_contact);
		et_phone = (EditText)findViewById(R.id.tv_contact);
		tvNum = (TextView) findViewById(R.id.tv_num);
		tvNum.setVisibility(View.GONE);
		btnRegist.setOnClickListener(this);
		btnContact.setOnClickListener(this);

		loadSharePrefrence();
		setSharePrefrence();
		initSDK();
	}


	private void initSDK() {
		// 初始化短信SDK
		SMSSDK.initSDK(this, APPKEY, APPSECRET);
		final Handler handler = new Handler(this);
		EventHandler eventHandler = new EventHandler() {
			public void afterEvent(int event, int result, Object data) {
				Message msg = new Message();
				msg.arg1 = event;
				msg.arg2 = result;
				msg.obj = data;
				handler.sendMessage(msg);
			}
		};
		// 注册回调监听接口
		SMSSDK.registerEventHandler(eventHandler);
		ready = true;

		// 获取新好友个数
		showDialog();
		SMSSDK.getNewFriendsCount();

	}

	private void loadSharePrefrence() {
		SharedPreferences p = getSharedPreferences("SMSSDK_SAMPLE", Context.MODE_PRIVATE);
		APPKEY = p.getString("APPKEY", APPKEY);
		APPSECRET = p.getString("APPSECRET", APPSECRET);
	}

	private void setSharePrefrence() {
		SharedPreferences p = getSharedPreferences("SMSSDK_SAMPLE", Context.MODE_PRIVATE);
		Editor edit = p.edit();
		edit.putString("APPKEY", APPKEY);
		edit.putString("APPSECRET", APPSECRET);
		edit.commit();
	}

	protected void onDestroy() {
		if (ready) {
			// 销毁回调监听接口
			SMSSDK.unregisterAllEventHandler();
		}
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (ready) {
			// 获取新好友个数
			showDialog();
			SMSSDK.getNewFriendsCount();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_bind_phone:
			String code = "86";
			String phone = "";
			phone = et_phone.getText().toString();
			if (!checkPhoneNum(phone, code)) {
				Toast.makeText(getApplicationContext(), "请输入正确的号码", 1000).show();
				return;
			}
			SMSSDK.getVerificationCode(code,phone.trim(), null);
			Toast.makeText(getApplicationContext(), "骚扰短信已发送", 1000).show();
			break;
		case R.id.rl_contact:
			tvNum.setVisibility(View.GONE);
			// 打开通信录好友列表页面
			ContactsPage contactsPage = new ContactsPage();
			contactsPage.show(this);
			break;
		}
	}
	
	/** 检查电话号码 */
	private boolean checkPhoneNum(String phone, String code) {
		if (code.startsWith("+")) {
			code = code.substring(1);
		}

		if (TextUtils.isEmpty(phone)) {
			return false;
		}
		
		if(phone.length() != 11) {
			return false;
		}

		return true;
	}

	public boolean handleMessage(Message msg) {
		if (pd != null && pd.isShowing()) {
			pd.dismiss();
		}

		int event = msg.arg1;
		int result = msg.arg2;
		Object data = msg.obj;
		if (event == SMSSDK.EVENT_SUBMIT_USER_INFO) {
			// 短信注册成功后，返回MainActivity,然后提示新好友
			if (result == SMSSDK.RESULT_COMPLETE) {
				Toast.makeText(this, R.string.smssdk_user_info_submited, Toast.LENGTH_SHORT).show();
			} else {
				((Throwable) data).printStackTrace();
			}
		} else if (event == SMSSDK.EVENT_GET_NEW_FRIENDS_COUNT){
			if (result == SMSSDK.RESULT_COMPLETE) {
				refreshViewCount(data);
			} else {
				((Throwable) data).printStackTrace();
			}
		}
		return false;
	}
	// 更新，新好友个数
	private void refreshViewCount(Object data){
		int newFriendsCount = 0;
		try {
			newFriendsCount = Integer.parseInt(String.valueOf(data));
		} catch (Throwable t) {
			newFriendsCount = 0;
		}
		if(newFriendsCount > 0){
			tvNum.setVisibility(View.VISIBLE);
			tvNum.setText(String.valueOf(newFriendsCount));
		}else{
			tvNum.setVisibility(View.GONE);
		}
		if (pd != null && pd.isShowing()) {
			pd.dismiss();
		}
	}
	// 弹出加载框
	private void showDialog(){
		if (pd != null && pd.isShowing()) {
			pd.dismiss();
		}
		pd = CommonDialog.ProgressDialog(this);
		pd.show();
	}
//	// 提交用户信息
//	private void registerUser(String country, String phone) {
//		Random rnd = new Random();
//		int id = Math.abs(rnd.nextInt());
//		String uid = String.valueOf(id);
//		String nickName = "SmsSDK_User_" + uid;
//		String avatar = AVATARS[id % 12];
//		SMSSDK.submitUserInfo(uid, nickName, avatar, country, phone);
//	}

}
