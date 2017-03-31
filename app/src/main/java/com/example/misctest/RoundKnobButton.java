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

public class RoundKnobButton extends RelativeLayout implements OnGestureListener {
	private static final String TAG = "RoundKnobButton";
	private  final boolean DBG = false;

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
    private float mKnobPostion;

	
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
        mPrevPos.x = x;
        mPrevPos.y = y;
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
        if (DBG) Log.d ("ROT" , "whichQuadrant() = " + whichQuadrant(x,y));
		return true;
	}

	public void setRotorPosAngle(float deg) {
		Matrix matrix=new Matrix();
		ivRotor.setScaleType(ScaleType.MATRIX);
		matrix.postRotate((float) deg, m_nWidth/2, m_nHeight/2);//getWidth()/2, getHeight()/2);
		ivRotor.setImageMatrix(matrix);
	}
	
	public void setRotorPercentage(int percentage) {
		int posDegree = percentage * 3 - 150;
        mKnobPostion  = posDegree;
		setRotorPosAngle(posDegree);
	}

	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		float x = e2.getX() / ((float) getWidth());
		float y = e2.getY() / ((float) getHeight());
		mCurrPos.x = x;
		mCurrPos.y = y;

		float prevPos = cartesianToPolar(1 - mPrevPos.x, 1 - mPrevPos.y);
        float currPos = cartesianToPolar(1 - x, 1 - y);// 1- to correct our custom axis direction
		float diff = currPos - prevPos;

		boolean isRotatingClockwise = isRotatingClockwise(mPrevPos, mCurrPos);

		if (isRotatingClockwise) {
			if (prevPos > 0 && currPos < 0) {
				diff += 360;
			}
		} else {
			if (prevPos < 0 && currPos > 0) {
				diff -= 360;
			}
		}

        mKnobPostion += diff;

		if (DBG) Log.d (TAG, "diff = " + diff + ", knob position = " + mKnobPostion);

        if (mKnobPostion < -150) {
            mKnobPostion = -150;
        }

		if (mKnobPostion == -150) {
			if (!isRotatingClockwise) {
				if (DBG) Log.d(TAG, "limited 0");
				mPrevPos.x = mCurrPos.x;
				mPrevPos.y = mCurrPos.y;
				mExceedLimit = true;
				setRotorPosAngle(mKnobPostion);
				mPercent = 0;
				if (m_listener != null) m_listener.onRotate(mPercent);
				return true;
			}
		}

        if (mKnobPostion > 150) {
            mKnobPostion = 150;
        }

		if (mKnobPostion == 150) {
			if (isRotatingClockwise) {
				if (DBG) Log.d(TAG, "limited 100");
				mPrevPos.x = mCurrPos.x;
				mPrevPos.y = mCurrPos.y;
				mExceedLimit = true;
				setRotorPosAngle(mKnobPostion);
				mPercent = 100;
				if (m_listener != null) m_listener.onRotate(mPercent);
				return true;
			}
		}

		// deny full rotation, start start and stop point, and get a linear scale
		if (mKnobPostion >= -150 &&  mKnobPostion <= 150) {
			// rotate our imageview
			setRotorPosAngle(mKnobPostion);
			// get a linear scale
			float scaleDegrees = mKnobPostion + 150; // given the current parameters, we go from 0 to 300
			// get position percent
			mPercent = (int) (scaleDegrees / 3);
			if (m_listener != null) m_listener.onRotate(mPercent);
			mPrevPos.x = mCurrPos.x;
			mPrevPos.y = mCurrPos.y;
			return true; //consumed
		} else {
			mPrevPos.x = mCurrPos.x;
			mPrevPos.y = mCurrPos.y;
			return false;
		}
	}

	public void onShowPress(MotionEvent e) { }
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

	private boolean currResult = true;
    private boolean prevResult = true;

    private boolean isDirectionChanged() {
        return !(prevResult == currResult);
    }

	boolean isRotatingClockwise(Position p, Position c) {
        prevResult = currResult;
        float prevPos = cartesianToPolar(1 - p.x, 1 - p.y);
        if (prevPos < 0) prevPos = 360 + prevPos;

        float currPos = cartesianToPolar(1 - c.x, 1 - c.y);
        if (currPos < 0) currPos = 360 + currPos;

		if (DBG) Log.d("ROT", "mPrevPos(x, y) = (" + mPrevPos.x + ", " + mPrevPos.y + ")");
		if (DBG) Log.d("ROT", "mCurrPos(x, y) = (" + mCurrPos.x + ", " + mCurrPos.y + ")");
        float diff = currPos - prevPos;
		if (DBG) Log.d(TAG, "diff = " + diff);

        if (whichQuadrant(p.x, p.y) == Quadrant.FORTH) {
            if (whichQuadrant(c.x, c.y) == Quadrant.FIRST) {
                currResult = true;
				if (DBG) {
					if (currResult != prevResult) {
						mToneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
					}
				}
                return true;
            }
        }

        if (whichQuadrant(c.x, c.y) == Quadrant.FORTH) {
            if (whichQuadrant(p.x, p.y) == Quadrant.FIRST) {
				if (DBG) Log.d(TAG, "move first to forth quadrant");
                currResult = false;
				if (DBG) {
					if (currResult != prevResult) {
						mToneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
					}
				}
                return false;
            }
        }

        if (diff >= 0 ) {
            currResult = true;
			if (DBG) {
				if (currResult != prevResult) {
					mToneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
				}
			}
			if (DBG) Log.d(TAG, "diff > 0 return true ");
            return true;
        }

        currResult = false;
		if (DBG) {
			if (currResult != prevResult) {
				mToneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
			}
		}

		if (DBG) Log.d(TAG, "return false ");
		return false;
	}

	Quadrant whichQuadrant(float x, float y) {
        Quadrant quad = Quadrant.FIRST;

        if (x <= 0.5 && y <= 0.5) quad = Quadrant.FORTH;
		else if (x >= 0.5 && y <= 0.5) quad = Quadrant.FIRST;
		else if (x >= 0.5 && y >= 0.5 ) quad = Quadrant.SECOND;
		else if (x <= 0.5 && y >= 0.5) quad = Quadrant.THIRD;

		if (DBG) Log.d(TAG, "quadrant = " + quad );
        return quad;
	}
}
