package jp.ddo.dekuyou.liveview.plugins.twittertl;

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.http.AccessToken;
import twitter4j.http.OAuthAuthorization;
import twitter4j.http.RequestToken;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.WindowManager.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.admob.android.ads.AdView;

public class TwitterTLOAuth extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);



		
		TextView tv = new TextView(this);
		tv.setText("OAuth");
		setContentView(tv);
		try {

			doOAuth();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
	}

	RequestToken req;
	OAuthAuthorization oath;

	Twitter twitter = new TwitterFactory().getOAuthAuthorizedInstance(
			Const.CONSUMER_KEY, Const.CONSUMER_SERCRET);

	// 認証メソッド
	private void doOAuth() throws Exception {
		req = twitter.getOAuthRequestToken(Const.CALLBACK_URL);
		String url = req.getAuthorizationURL();




		// テストモード
//		AdManager.setTestDevices( new String[] {
//			AdManager.TEST_EMULATOR,"851F8BBED56538E3534A33CC1AD5543B"
//		} );
		LinearLayout linearLayout = new LinearLayout(this);
		AdView mAdView = new AdView(this);
		mAdView.requestFreshAd();
		linearLayout.addView(mAdView);
		addContentView(linearLayout, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

		
		// 認証情報作成
		// ブラウザを起動して認証ページへ
		this.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));

		
	}
	
	
	@Override
	protected void onNewIntent(Intent intent){
		
		super.onNewIntent(intent);
		
		// ■暗証番号の入力を求めるダイアログ

		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("TwitTL");
		alert.setMessage("do save Access Token?");

		// Set an EditText view to get user input
		// final EditText input = new EditText(this);
		// alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Do something with value!
				// setCode(input.getText().toString().trim());
				setCode();
			}
		});

		alert.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Canceled.
						 showAdd();
					}
				});

		alert.show();
		
		
	}
	private void setCode() {

		// ■アクセストークンの作成（認証）
		ObjectOutputStream os = null;
		try {
			// AccessToken accessToken = twitter.getOAuthAccessToken(req,
			// pin);
			AccessToken accessToken = twitter.getOAuthAccessToken(req);
			// System.out.println("AccessToken\t" + accessToken);

			// AccessTokenを保存

			OutputStream out = openFileOutput(Const.FILE_NAME, MODE_PRIVATE);
			os = new ObjectOutputStream(out);
			os.writeObject(accessToken);

			TextView tv = new TextView(this);
			tv.setText("storeAccessToken success. ");
			setContentView(tv);

		} catch (IOException e) {
			TextView tv = new TextView(this);
			tv.setText("storeAccessToken Failed ! ");
			setContentView(tv);

			// e.printStackTrace();
		} catch (TwitterException e) {
			TextView tv = new TextView(this);
			tv.setText("getOAuthAccessToken Failed ! ");
			setContentView(tv);

			System.out.println(e.getStatusCode()); // 拒否されると401
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			 showAdd();
		}

	}

	// アクセストークンを保存するファイル名を生成する
	File createAccessTokenFileName(String name) {
		String s = name + ".dat";
		return new File(s);
	}
	
	private void showAdd(){
		// help
		this.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://dekuyou.ddo.jp/lvtwittl/lvtwittl_help_m.html")));

	}
}
