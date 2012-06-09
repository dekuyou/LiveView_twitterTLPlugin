
package jp.ddo.dekuyou.liveview.plugins.twittertl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.List;

import twitter4j.DirectMessage;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.http.AccessToken;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.sonyericsson.extras.liveview.plugins.AbstractPluginService;
import com.sonyericsson.extras.liveview.plugins.PluginConstants;
import com.sonyericsson.extras.liveview.plugins.PluginUtils;

public class TwitterTLPluginService extends AbstractPluginService {

	private static final int ROWS = 10;
	// Our handler.
	private Handler mHandler = null;


	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);

		// Create handler.
		if (mHandler == null) {
			mHandler = new Handler();
		}

	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		stopWork();
	}

	/**
	 * Plugin is sandbox.
	 */
	protected boolean isSandboxPlugin() {
		return true;
	}

	enum TL{ HOME, Mentions, DMs; };

	
	List<Status> statuses =null;
	List<DirectMessage> dms = null;
	int no = 0;
	AccessToken accessToken = null;
	Twitter twitter = null;
	Paging paging = new Paging(1, ROWS);
	int page = 1;
	
	
	TL nowMode = TL.HOME;
	
	/**
	 * Must be implemented. Starts plugin work, if any.
	 */
	protected void startWork() {

		// Check if plugin is enabled.
//		if (mSharedPreferences.getBoolean(
//				"HOMETLEnabled", false)) {
			// Do stuff.

		try {
			mLiveViewAdapter.clearDisplay(mPluginId);
		} catch (Exception e) {
			Log.e(PluginConstants.LOG_TAG, "Failed to clear display.");
		}
		PluginUtils.sendTextBitmap(mLiveViewAdapter, mPluginId, "TwitTL!", 128,
				10);

		InputStream in;
		try {
			in = openFileInput(Const.FILE_NAME);
			ObjectInputStream ois;
			ois = new ObjectInputStream(in);
			accessToken = (AccessToken) ois.readObject();

			twitter = new TwitterFactory().getOAuthAuthorizedInstance(
					Const.CONSUMER_KEY, Const.CONSUMER_SERCRET, accessToken);
			if (twitter == null) {
				throw new FileNotFoundException();
			}
			
			if (((statuses == null && (TL.HOME.equals(nowMode) || TL.Mentions
					.equals(nowMode)))
					|| (dms == null && TL.DMs.equals(nowMode))
					) || mSharedPreferences.getBoolean(
							"ReloadAtTheStart", true)) {
				statuses = null;
				dms = null;
				no = 0;
				paging = new Paging(1, ROWS);
				page = 1;

				getTimeline(paging);
			}
			doDraw();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			mHandler.postDelayed(new Runnable() {
				public void run() {
					// First message to LiveView
					try {
						mLiveViewAdapter.clearDisplay(mPluginId);
					} catch (Exception e) {
						Log.e(PluginConstants.LOG_TAG,
								"Failed to clear display.");
					}
					PluginUtils.sendTextBitmap(mLiveViewAdapter, mPluginId,
							"Please do OAuth!", 128, 10);
				}
			}, 1000);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO: handle exception
		}

//		} else {
//			mHandler.postDelayed(new Runnable() {
//				public void run() {
//					// First message to LiveView
//					try {
//						mLiveViewAdapter.clearDisplay(mPluginId);
//					} catch (Exception e) {
//						Log.e(PluginConstants.LOG_TAG,
//								"Failed to clear display.");
//					}
//					PluginUtils.sendTextBitmap(mLiveViewAdapter, mPluginId,
//							"TwitTL!", 128, 10);
//				}
//			}, 1000);
//		}
	}

	private void getTimeline(Paging paging) throws TwitterException {
		mLiveViewAdapter.vibrateControl(mPluginId, 0, 50);
		
		
		switch (nowMode) {
		case HOME:
			if (statuses == null) {
				statuses = twitter.getHomeTimeline(paging);

			} else {
				statuses.addAll(twitter.getHomeTimeline(paging));
			}
			break;
		case Mentions:
			if (statuses == null) {
				statuses = twitter.getMentions(paging);

			} else {
				statuses.addAll(twitter.getMentions(paging));
			}
			break;
		case DMs:
			if (dms == null) {
				dms = twitter.getDirectMessages(paging);

			} else {
				dms.addAll(twitter.getDirectMessages(paging));
			}

		default:
			break;
		}

	}

	private void doDraw() throws IllegalStateException, TwitterException {
		String modeString = "";
		String screenNameString = "";
		String msgString = "";
		switch (nowMode) {
		case HOME:
			if(statuses == null || statuses.size() == 0){
				return;
			}
			modeString = "Home";
			screenNameString = statuses.get(no).getUser().getScreenName(); 
			msgString = statuses.get(no).getText();
			break;

		case Mentions:
			if(statuses == null || statuses.size() == 0){
				return;
			}
//			modeString = "@"+twitter.getScreenName();
			modeString = "@Mt";
			screenNameString = statuses.get(no).getUser().getScreenName(); 
			msgString = statuses.get(no).getText();
					
			break;
		case DMs:
			if(dms == null || dms.size() == 0){
				return;
			}
			modeString = "DMs";
			screenNameString = dms.get(no).getSenderScreenName(); 
			msgString = dms.get(no).getText();
			
			break;
		default:
			break;
		}
		
		String msg = modeString + " " + new Integer(no).toString() + " : " + screenNameString + "\n" + msgString.replaceAll("\n", " ");
		PluginUtils.sendTextBitmap(mLiveViewAdapter, mPluginId,
				msg, 128, 10, 128);
	}

	private void doReTweet() {
		try {

			// TODO Auto-generated method stub
			Status tmpStts = statuses.get(no);
			if (!tmpStts.isRetweetedByMe()) {
				mLiveViewAdapter.vibrateControl(mPluginId, 0, 50);

				tmpStts = twitter.retweetStatus(tmpStts.getId());
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Must be implemented. Stops plugin work, if any.
	 */
	protected void stopWork() {
//		super.stopForeground(true);
	}

	/**
	 * Must be implemented.
	 * 
	 * PluginService has done connection and registering to the LiveView
	 * Service.
	 * 
	 * If needed, do additional actions here, e.g. starting any worker that is
	 * needed.
	 */
	protected void onServiceConnectedExtended(ComponentName className,
			IBinder service) {

	}

	/**
	 * Must be implemented.
	 * 
	 * PluginService has done disconnection from LiveView and service has been
	 * stopped.
	 * 
	 * Do any additional actions here.
	 */
	protected void onServiceDisconnectedExtended(ComponentName className) {

	}

	/**
	 * Must be implemented.
	 * 
	 * PluginService has checked if plugin has been enabled/disabled.
	 * 
	 * The shared preferences has been changed. Take actions needed.
	 */
	protected void onSharedPreferenceChangedExtended(SharedPreferences prefs,
			String key) {

	}

	protected void startPlugin() {
		Log.d(PluginConstants.LOG_TAG, "startPlugin");
		startWork();
	}

	protected void stopPlugin() {
		Log.d(PluginConstants.LOG_TAG, "stopPlugin");
		stopWork();
	}

	
	boolean execute = false;
	protected void button(String buttonType, boolean doublepress,
			boolean longpress) {
		Log.d(PluginConstants.LOG_TAG, "button - type " + buttonType
				+ ", doublepress " + doublepress + ", longpress " + longpress);
		
		if(execute){
			return; // 処理中であれば他のボタン処理は受け付けない。
		}
		
		execute =true;
		
		if(twitter == null){
			return;
		}
		
		try {

			if (buttonType.equalsIgnoreCase(PluginConstants.BUTTON_UP)) {

				
				if (no > 0) {
					no = no - 1;
				} else {
					no = 0;
					statuses = null;
					dms = null;
					page = 1;
					paging = new Paging(page,ROWS);
					getTimeline(paging);
				}
				doDraw();

			} else if (buttonType.equalsIgnoreCase(PluginConstants.BUTTON_DOWN)) {

				switch (nowMode) {
				case DMs:
					if (dms.size() - 1 > no) {
						no = no + 1;
					} else {
						page = page +1;
						paging = new Paging(page, ROWS);
//						paging.setSinceId(dms.get(no).getId());
						getTimeline(paging);

						no = no + 1;

						if (dms.size() - 1 <= no) {
							no = dms.size() - 1;
					
						}
//						no = statuses.size() - 1;

					}					
					break;

				default:
					if (statuses.size() - 1 > no) {
						no = no + 1;
					} else {
						page = page +1;
						paging = new Paging(page, ROWS);
//						paging.setMaxId(statuses.get(no).getId());
						getTimeline(paging);

						no = no + 1;

						if (statuses.size() - 1 <= no) {
							no = statuses.size() - 1;
					
						}
//						no = statuses.size() - 1;

					}
					break;
				}
				doDraw();

			} else if (buttonType
					.equalsIgnoreCase(PluginConstants.BUTTON_RIGHT)) {
				
				switch (nowMode) {
				case HOME:
					// retweet
					if(longpress){
						doReTweet();
						
					}
					break;
				case Mentions:
					// retweet
					if(longpress){
						doReTweet();
						
					}
					break;
				case DMs:
					
					break;

				default:
					break;
				}
				

			} else if (buttonType.equalsIgnoreCase(PluginConstants.BUTTON_LEFT)) {
				
				mLiveViewAdapter.vibrateControl(mPluginId, 0, 50);
				statuses = null;
				
				switch (nowMode) {
				case HOME:
					nowMode = TL.Mentions;
					statuses = null;
					
					break;
				case Mentions:
					nowMode = TL.DMs;
					dms = null;
					
					break;
				case DMs:
					nowMode = TL.HOME;
					statuses = null;
					
					break;

				default:
					break;
				}
				page = 1;
				paging = new Paging(page, ROWS);
				no = 0;
				getTimeline(paging);
				doDraw();

			} else if (buttonType
					.equalsIgnoreCase(PluginConstants.BUTTON_SELECT)) {
				statuses = null;
				dms =null;
				page = 1;
				paging = new Paging(page,ROWS);
				getTimeline(paging);
				no = 0;
				doDraw();
			}
		} catch (TwitterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		execute = false;
	}

	protected void displayCaps(int displayWidthPx, int displayHeigthPx) {
		Log.d(PluginConstants.LOG_TAG, "displayCaps - width " + displayWidthPx
				+ ", height " + displayHeigthPx);
	}

	protected void onUnregistered() throws RemoteException {
		Log.d(PluginConstants.LOG_TAG, "onUnregistered");
		stopWork();
	}

	protected void openInPhone(String openInPhoneAction) {
		Log.d(PluginConstants.LOG_TAG, "openInPhone: " + openInPhoneAction);
	}

	protected void screenMode(int mode) {
		Log.d(PluginConstants.LOG_TAG, "screenMode: screen is now "
				+ ((mode == 0) ? "OFF" : "ON"));

		if (mode == PluginConstants.LIVE_SCREEN_MODE_OFF) {
//			stopWork();
		}
	}
	
	

}
