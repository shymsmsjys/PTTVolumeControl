package com.example.misctest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;

/*
File:              RoundKnobButton
Version:           1.0.0
Release Date:      November, 2013
License:           GPL v2
Description:	   A round knob button to control volume and toggle between two states

****************************************************************************
Copyright (C) 2013 Radu Motisan  <radu.motisan@gmail.com>

http://www.pocketmagic.net

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
****************************************************************************/

public class RoundKnobButton extends RelativeLayout implements OnGestureListener {

	private GestureDetector gestureDetector;
	private float mAngleDown , mAngleUp;
	private ImageView ivRotor;
	private Bitmap bmpRotorOn , bmpRotorOff;
	private boolean mState = false;
	private int m_nWidth = 0, m_nHeight = 0;
	private Position mPrevPos;
	private Position mCurrPos;
    private ToneGenerator mToneGenerator;
    private int mPercent;
    private boolean mExceedLimit;

	
	interface RoundKnobButtonListener {
		public void onStateChange(boolean newstate) ;
		public void onRotate(int percentage);
	}
	
	private RoundKnobButtonListener m_listener;
	
	public void SetListener(RoundKnobButtonListener l) {
		m_listener = l;
	}
	
	public void SetState(boolean state) {
		mState = state;
		ivRotor.setImageBitmap(state?bmpRotorOn:bmpRotorOff);
	}
	
	public RoundKnobButton(Context context, int back, int rotoron, int rotoroff, final int w, final int h) {
		super(context);
		// we won't wait for our size to be calculated, we'll just store out fixed size
		m_nWidth = w; 
		m_nHeight = h;
		// create stator
		ImageView ivBack = new ImageView(context);
		ivBack.setImageResource(back);
		RelativeLayout.LayoutParams lp_ivBack = new RelativeLayout.LayoutParams(
				w,h);
		lp_ivBack.addRule(RelativeLayout.CENTER_IN_PARENT);
		addView(ivBack, lp_ivBack);
		// load rotor images
		Bitmap srcon = BitmapFactory.decodeResource(context.getResources(), rotoron);
		Bitmap srcoff = BitmapFactory.decodeResource(context.getResources(), rotoroff);
	    float scaleWidth = ((float) w) / srcon.getWidth();
	    float scaleHeight = ((float) h) / srcon.getHeight();
	    Matrix matrix = new Matrix();
	    matrix.postScale(scaleWidth, scaleHeight);
		    
		bmpRotorOn = Bitmap.createBitmap(
				srcon, 0, 0, 
				srcon.getWidth(),srcon.getHeight() , matrix , true);
		bmpRotorOff = Bitmap.createBitmap(
				srcoff, 0, 0, 
				srcoff.getWidth(),srcoff.getHeight() , matrix , true);
		// create rotor
		ivRotor = new ImageView(context);
		ivRotor.setImageBitmap(bmpRotorOn);
		RelativeLayout.LayoutParams lp_ivKnob = new RelativeLayout.LayoutParams(w,h);//LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		lp_ivKnob.addRule(RelativeLayout.CENTER_IN_PARENT);
		addView(ivRotor, lp_ivKnob);
		// set initial state
		SetState(mState);
		// enable gesture detector
		gestureDetector = new GestureDetector(getContext(), this);
        mPrevPos = new Position();
        mCurrPos = new Position();
        mToneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        mExceedLimit = true;
	}
	
	/**
	 * math..
	 * @param x
	 * @param y
	 * @return
	 */
	private float cartesianToPolar(float x, float y) {
		return (float) -Math.toDegrees(Math.atan2(x - 0.5f, y - 0.5f));
	}

	
	@Override public boolean onTouchEvent(MotionEvent event) {
		if (gestureDetector.onTouchEvent(event)) return true;
		else return super.onTouchEvent(event);
	}
	
	public boolean onDown(MotionEvent event) {
		float x = event.getX() / ((float) getWidth());
		float y = event.getY() / ((float) getHeight());
		mAngleDown = cartesianToPolar(1 - x, 1 - y);// 1- to correct our custom axis direction
		return true;
	}
	
	public boolean onSingleTapUp(MotionEvent e) {
		float x = e.getX() / ((float) getWidth());
		float y = e.getY() / ((float) getHeight());
		mAngleUp = cartesianToPolar(1 - x, 1 - y);// 1- to correct our custom axis direction
		
		// if we click up the same place where we clicked down, it's just a button press
		if (! Float.isNaN(mAngleDown) && ! Float.isNaN(mAngleUp) && Math.abs(mAngleUp-mAngleDown) < 10) {
			SetState(!mState);
			if (m_listener != null) m_listener.onStateChange(mState);
		}
        Log.d ("ROT" , "whichQuadrant() = " + whichQuadrant(x,y));
		return true;
	}

	public void setRotorPosAngle(float deg) {

		if (deg >= 210 || deg <= 150) {
			if (deg > 180) deg = deg - 360;
			Matrix matrix=new Matrix();
			ivRotor.setScaleType(ScaleType.MATRIX);   
			matrix.postRotate((float) deg, m_nWidth/2, m_nHeight/2);//getWidth()/2, getHeight()/2);
			ivRotor.setImageMatrix(matrix);
		}
	}
	
	public void setRotorPercentage(int percentage) {
		int posDegree = percentage * 3 - 150;
		if (posDegree < 0) posDegree = 360 + posDegree;
		setRotorPosAngle(posDegree);
	}
	
	
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		float x = e2.getX() / ((float) getWidth());
		float y = e2.getY() / ((float) getHeight());

        float rotDegrees = cartesianToPolar(1 - x, 1 - y);// 1- to correct our custom axis direction

        mCurrPos.x = x;
        mCurrPos.y = y;
//        Log.d("ROT", "mPrevPos(x, y) = (" + mPrevPos.x + ", " + mPrevPos.y + ")");
//        Log.d("ROT", "mCurrPos(x, y) = (" + mCurrPos.x + ", " + mCurrPos.y + ")");
//
		Log.d ("ROT", "isRatateClockwise?  " +  isRotatingClockwise(mPrevPos, mCurrPos));

        float prevPos = cartesianToPolar(1 - mPrevPos.x, 1 - mPrevPos.y);
        if (prevPos < 0) prevPos = 360 + prevPos;

        float currPos = cartesianToPolar(1 - mCurrPos.x, 1 - mCurrPos.y);
        if (currPos < 0) currPos = 360 + currPos;

        int diff = (int)(Math.abs(currPos - prevPos));

        // 급격히 움직일 경우 각도 차이가 꺼서 아래 조건에 진입 시키기 위한 계산
        int min = 0 + diff / 3;
        int max = 100 - diff / 3;

        // 0 또는 100을 넘어서 회전하지 않도록 하기 위한 조건
        if (rotDegrees < -110) {
            if (mPercent <= min && !isRotatingClockwise(mPrevPos, mCurrPos)) {
                Log.d("ROT", "limited 0");
                mPrevPos.x = mCurrPos.x;
                mPrevPos.y = mCurrPos.y;
                mExceedLimit = true;
                mPercent = 0;
                if (m_listener != null) m_listener.onRotate(mPercent);
                return false;
            }
        }

        if (rotDegrees > 110) {
            if (mPercent >= max && isRotatingClockwise(mPrevPos, mCurrPos)) {
                Log.d("ROT", "limited 100");
                mPrevPos.x = mCurrPos.x;
                mPrevPos.y = mCurrPos.y;
                mExceedLimit = true;
                mPercent = 100;
                if (m_listener != null) m_listener.onRotate(mPercent);
                return false;
            }
        }

		if (! Float.isNaN(rotDegrees)) {
			// instead of getting 0-> 180, -180 0 , we go for 0 -> 360
			float posDegrees = rotDegrees;
			if (rotDegrees < 0) posDegrees = 360 + rotDegrees;

            // 최대/최소를 넘긴 상황에서 방향이 바뀌었나?
            // 0보다 크거나 100 보다 작으면 진행 아니면 return

            if (mPercent <= 1)
            {
                if (isRotatingClockwise(mPrevPos, mCurrPos)
                        && posDegrees >= 210) {
                    mExceedLimit = false;
                }
            }

            if (mPercent >= 99)
            {
                if (!isRotatingClockwise(mPrevPos, mCurrPos)
                        && posDegrees <= 150) {
                    mExceedLimit = false;
                }
            }

            if (mExceedLimit) {
                mPrevPos.x = mCurrPos.x;
                mPrevPos.y = mCurrPos.y;
                return false;
            }


            // deny full rotation, start start and stop point, and get a linear scale
			if (posDegrees > 210 || posDegrees < 150) {
				// rotate our imageview
				setRotorPosAngle(posDegrees);
				// get a linear scale
				float scaleDegrees = rotDegrees + 150; // given the current parameters, we go from 0 to 300
				// get position percent
				mPercent = (int) (scaleDegrees / 3);
				if (m_listener != null) m_listener.onRotate(mPercent);
                mPrevPos.x = mCurrPos.x;
                mPrevPos.y = mCurrPos.y;
                Log.d("ROT", "mPrevPos = " + mPrevPos);

				return true; //consumed
			} else {
                mPrevPos.x = mCurrPos.x;
                mPrevPos.y = mCurrPos.y;
                return false;
            }
		} else {
            mPrevPos.x = mCurrPos.x;
            mPrevPos.y = mCurrPos.y;
            return false; // not consumed
        }
	}

	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}
	public boolean onFling(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3) { return false; }

	public void onLongPress(MotionEvent e) {	}

	private class Position {
		float x;
		float y;
	}

	enum Quadrant {
		FIRST,
		SECOND,
		THIRD,
		FORTH
	}

	private boolean currDirection = true;
    private boolean prevDirection = true;

    private boolean isDirectionChanged() {
        return !(prevDirection == currDirection);
    }

	boolean isRotatingClockwise(Position p, Position c) {
//        p.x = -0.2177445f; p.y = 0.5021704f;
//        c.x = -0.2177445f; c.y = 0.49648315f;
        prevDirection = currDirection;
        float prevPos = cartesianToPolar(1 - p.x, 1 - p.y);
        if (prevPos < 0) prevPos = 360 + prevPos;

        float currPos = cartesianToPolar(1 - c.x, 1 - c.y);
        if (currPos < 0) currPos = 360 + currPos;

        Log.d("ROT", "mPrevPos(x, y) = (" + mPrevPos.x + ", " + mPrevPos.y + ")");
        Log.d("ROT", "mCurrPos(x, y) = (" + mCurrPos.x + ", " + mCurrPos.y + ")");
        float diff = currPos - prevPos;
        Log.d("ROT", "diff = " + diff);

        if (whichQuadrant(p.x, p.y) == Quadrant.FORTH) {
            if (whichQuadrant(c.x, c.y) == Quadrant.FIRST) {
                currDirection = true;
                if (currDirection != prevDirection) {
                    mToneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
                }

                return true;
            }
        }

        if (whichQuadrant(c.x, c.y) == Quadrant.FORTH) {
            if (whichQuadrant(p.x, p.y) == Quadrant.FIRST) {
                Log.d("ROT", "move first to forth quadrant");
                currDirection = false;
                if (currDirection != prevDirection) {
                    mToneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
                }

                return false;
            }
        }

        if (diff >= 0 ) {
            currDirection = true;
            if (currDirection != prevDirection) {
                mToneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
            }
            Log.d("ROT", "diff > 0 return true ");
            return true;
        }

        currDirection = false;
        if (currDirection != prevDirection) {
            mToneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
        }
        Log.d("ROT", "return false ");
		return false;
	}

	Quadrant whichQuadrant(float x, float y) {
        Quadrant quad = Quadrant.FIRST;

        if (x <= 0.5 && y <= 0.5) quad = Quadrant.FORTH;
		else if (x >= 0.5 && y <= 0.5) quad = Quadrant.FIRST;
		else if (x >= 0.5 && y >= 0.5 ) quad = Quadrant.SECOND;
		else if (x <= 0.5 && y >= 0.5) quad = Quadrant.THIRD;

        Log.d("ROT", "quatrant = " + quad );
        return quad;
	}


}
