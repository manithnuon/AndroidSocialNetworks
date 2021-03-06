package com.androidsocialnetworks.lib.impl;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.androidsocialnetworks.lib.MomentUtil;
import com.androidsocialnetworks.lib.SocialNetwork;
import com.androidsocialnetworks.lib.SocialNetworkException;
import com.androidsocialnetworks.lib.SocialPerson;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.plus.PlusClient;
import com.google.android.gms.plus.model.people.Person;

import java.io.File;
import java.util.UUID;

public class GooglePlusSocialNetwork extends SocialNetwork
        implements GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener {

    public static final int ID = 3;

    private static final String TAG = GooglePlusSocialNetwork.class.getSimpleName();
    // max 16 bit to use in startActivityForResult
    private static final int REQUEST_AUTH = UUID.randomUUID().hashCode() & 0xFFFF;

    private PlusClient mPlusClient;
    private ConnectionResult mConnectionResult;

    private boolean mConnectRequested;

    private Handler mHandler = new Handler();

    public GooglePlusSocialNetwork(Fragment fragment) {
        super(fragment);
    }

    @Override
    public boolean isConnected() {
        return mPlusClient.isConnected();
    }

    @Override
    public void requestLogin() throws SocialNetworkException {
        if (isConnected()) {
            if (mOnLoginCompleteListener != null) {
                mOnLoginCompleteListener.onLoginSuccess(getID());
            }

            return;
        }

        mConnectRequested = true;

        try {
            mConnectionResult.startResolutionForResult(mSocialNetworkManager.getActivity(), REQUEST_AUTH);
        } catch (Exception e) {
            Log.e(TAG, "ERROR", e);
            if (!mPlusClient.isConnecting()) {
                mPlusClient.connect();
            }
        }
    }

    @Override
    public void logout() {
        mConnectRequested = false;

        if (mPlusClient.isConnected()) {
            mPlusClient.clearDefaultAccount();
            mPlusClient.disconnect();
            mPlusClient.connect();
        }
    }

    @Override
    public int getID() {
        return ID;
    }

    @Override
    public void requestPerson() throws SocialNetworkException {
        Person person = mPlusClient.getCurrentPerson();

        if (person == null) {
            if (mOnRequestSocialPersonListener != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mOnRequestSocialPersonListener.onRequestSocialPersonFailed(getID(), "Can't get person");
                    }
                });
            }

            return;
        }

        final SocialPerson socialPerson = new SocialPerson();
        socialPerson.id = person.getId();
        socialPerson.name = person.getDisplayName();

        Person.Image image = person.getImage();
        if (image != null) {
            String imageURL = image.getUrl();

            if (imageURL != null) {
                socialPerson.avatarURL = imageURL;
            }
        }

        if (mOnRequestSocialPersonListener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mOnRequestSocialPersonListener.onRequestSocialPersonSuccess(getID(), socialPerson);
                }
            });
        }
    }

    @Override
    public void requestPostMessage(String message) throws SocialNetworkException {
        throw new SocialNetworkException("requestPostMessage isn't allowed for GooglePlusSocialNetwork");
    }

    @Override
    public void requestPostPhoto(File photo, String message) throws SocialNetworkException {
        throw new SocialNetworkException("requestPostPhoto isn't allowed for GooglePlusSocialNetwork");
    }

    @Override
    public void requestCheckIsFriend(String userID) throws SocialNetworkException {
        throw new SocialNetworkException("requestCheckIsFriend isn't allowed for GooglePlusSocialNetwork");
    }

    @Override
    public void requestAddFriend(String userID) throws SocialNetworkException {
        throw new SocialNetworkException("requestAddFriend isn't allowed for GooglePlusSocialNetwork");
    }

    @Override
    public void requestRemoveFriend(String userID) throws SocialNetworkException {
        throw new SocialNetworkException("requestRemoveFriend isn't allowed for GooglePlusSocialNetwork");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPlusClient = new PlusClient.Builder(mSocialNetworkManager.getActivity(), this, this)
                .setActions(MomentUtil.ACTIONS).build();
    }

    @Override
    public void onStart() {
        mPlusClient.connect();
    }

    @Override
    public void onStop() {
        if (mPlusClient.isConnected()) {
            mPlusClient.disconnect();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_AUTH) {
            if (resultCode == Activity.RESULT_OK && !mPlusClient.isConnected() && !mPlusClient.isConnecting()) {
                // This time, connect should succeed.
                mPlusClient.connect();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                if (mOnLoginCompleteListener != null) {
                    mOnLoginCompleteListener.onLoginFailed(getID(), "canceled");
                }
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (mConnectRequested) {
            if (mPlusClient.getCurrentPerson() != null) {
                if (mOnLoginCompleteListener != null) {
                    mOnLoginCompleteListener.onLoginSuccess(getID());
                }

                return;
            }

            if (mOnLoginCompleteListener != null) {
                mOnLoginCompleteListener.onLoginFailed(getID(), "get person == null");
            }
        }

        mConnectRequested = false;
    }

    @Override
    public void onDisconnected() {
        mConnectRequested = false;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        mConnectionResult = connectionResult;

        if (mConnectRequested && mOnLoginCompleteListener != null) {
            mOnLoginCompleteListener.onLoginFailed(getID(), "error: " + connectionResult.getErrorCode());
        }

        mConnectRequested = false;
    }

    /**
     * requestLogin is executing synchronously in GooglePlusSocialNetwork, so canceling
     * doesn't have any sence
     */
    @Override
    public void cancelLoginRequest() {

    }

    /**
     * requestPerson is executing synchronously in GooglePlusSocialNetwork, so canceling
     * doesn't have any sence
     */
    @Override
    public void cancelGetPersonRequest() {

    }

    /**
     * requestPostMessage isn't allowed for GooglePlusSocialNetwork
     */
    @Override
    public void cancelPostMessageRequest() {

    }

    /**
     * requestPostPhoto isn't allowed for GooglePlusSocialNetwork
     */
    @Override
    public void cancelPostPhotoRequest() {

    }

    /**
     * requestCheckIsFriend isn't allowed for GooglePlusSocialNetwork
     */
    @Override
    public void cancenCheckIsFriendRequest() {

    }

    /**
     * requestAddFriend isn't allowed for GooglePlusSocialNetwork
     */
    @Override
    public void cancelAddFriendRequest() {

    }

    /**
     * requestRemoveFriend isn't allowed for GooglePlusSocialNetwork
     */
    @Override
    public void cancenRemoveFriendRequest() {

    }
}
