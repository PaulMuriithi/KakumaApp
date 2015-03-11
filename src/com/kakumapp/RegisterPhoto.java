package com.kakumapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.cocosw.bottomsheet.BottomSheet;

@SuppressWarnings("deprecation")
public class RegisterPhoto extends ActionBarActivity implements
		SurfaceHolder.Callback, Camera.ShutterCallback, Camera.PictureCallback {

	private static final String DIRECTORY_IMAGES = "Kakuma";
	private static final String TAG = "RegisterPhoto";
	private static int IMAGE_MAX_SIZE = 1000;
	private Camera mCamera;
	private SurfaceView mPreview;
	private File photoFile;
	private Bitmap bitmap;
	private int currentCameraId = 0;
	private ActionBarActivity activity;
	// control buttons
	private ImageView flashImageView, switchImageView;
	// take photo
	private Button captureButton;
	private boolean hasFrontCamera;
	private String firstName, lastName, otherName, phoneNumber, countryCode,
			country;
	private ArrayList<String> places;
	private BottomSheet bottomSheet;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_photo);
		activity = this;

		mPreview = (SurfaceView) findViewById(R.id.camera_preview);
		switchImageView = (ImageView) findViewById(R.id.imageView_switch);
		flashImageView = (ImageView) findViewById(R.id.imageView_flash);
		flashImageView.setTag(R.drawable.ic_action_flash_automatic);

		mPreview.getHolder().addCallback(this);
		mPreview.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		// prefer starting with front camera if present
		if (hasAFrontCamera()) {
			currentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
			showFlash(false);
		} else {
			currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
			// hide switch imageview
			switchImageView.setVisibility(View.GONE);
		}
		try {
			mCamera = Camera.open(currentCameraId);
		} catch (Exception exception) {
			Log.e(TAG, "Exception " + exception.getLocalizedMessage());
			showErrorMessage("Camera", "Could not open the camera");
		}
		switchImageView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				switchCamera();
			}
		});

		// if flash is not available,hide the flash imageView
		if (!hasFlash()) {
			showFlash(false);
		}

		flashImageView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				int imageResource = R.drawable.ic_action_flash_automatic;
				Camera.Parameters parameters = mCamera.getParameters();
				int tag = (int) flashImageView.getTag();
				if (tag == R.drawable.ic_action_flash_automatic) {
					imageResource = R.drawable.ic_action_flash_on;
					parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
				} else if (tag == R.drawable.ic_action_flash_on) {
					imageResource = R.drawable.ic_action_flash_off;
					parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
				} else if (tag == R.drawable.ic_action_flash_off) {
					imageResource = R.drawable.ic_action_flash_automatic;
					parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
				}
				try {
					mCamera.setParameters(parameters);
					flashImageView.setTag(imageResource);
					flashImageView.setImageResource(imageResource);
				} catch (Exception exception) {
					Log.e(TAG, "Exception " + exception.getLocalizedMessage());
				}
			}
		});

		// Trap the capture button.
		captureButton = (Button) findViewById(R.id.button_capture);
		captureButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// get an image from the camera
				mCamera.takePicture(RegisterPhoto.this, null, null,
						RegisterPhoto.this);
			}
		});

		IMAGE_MAX_SIZE = getScreenSize()[1];

		// get data if passed
		Bundle bundle = getIntent().getExtras();
		if (bundle != null) {
			// for the phone number
			phoneNumber = bundle.getString("phone");
			countryCode = bundle.getString("code");
			firstName = bundle.getString("firstName");
			lastName = bundle.getString("lastName");
			otherName = bundle.getString("otherName");
			country = bundle.getString("country");
			places = bundle.getStringArrayList("places");
		}
	}

	private boolean hasFlash() {
		Camera.Parameters p = mCamera.getParameters();
		return p.getFlashMode() == null ? false : true;
	}

	private void showFlash(boolean flash) {
		if (flashImageView != null) {
			if (flash) {
				flashImageView.setVisibility(View.VISIBLE);
			} else {
				flashImageView.setVisibility(View.GONE);
			}
		}
	}

	/**
	 * Refresh the camera after a switch is called
	 */
	public void switchCamera() {
		mCamera.stopPreview();
		mCamera.release();

		if (hasFrontCamera) {
			if (currentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
				currentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
				showFlash(false);
			} else {
				currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
				showFlash(true);
			}
		} else {
			currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
			showFlash(true);
		}
		mCamera = Camera.open(currentCameraId);
		// Set the 'rotation' parameter
		Camera.Parameters params = mCamera.getParameters();
		params.setRotation(getRotationAngle());
		try {
			mCamera.setPreviewDisplay(mPreview.getHolder());
			mCamera.setParameters(params);
			mCamera.setDisplayOrientation(90);
			mCamera.startPreview();
		} catch (IOException e) {
			Log.e(TAG, "Exception " + e.getLocalizedMessage());
		}
	}

	/**
	 * Get Rotation Angle
	 */
	public int getRotationAngle() {
		Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
		Camera.getCameraInfo(currentCameraId, info);
		int rotation = getWindowManager().getDefaultDisplay().getRotation();
		int degrees = 0;
		switch (rotation) {
		// Natural orientation
		case Surface.ROTATION_0:
			degrees = 0;
			break;
		// Landscape left
		case Surface.ROTATION_90:
			degrees = 90;
			break;
		// Upside down
		case Surface.ROTATION_180:
			degrees = 180;
			break;
		// Landscape right
		case Surface.ROTATION_270:
			degrees = 270;
			break;
		}
		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
			// compensate the mirror
			result = (360 - result) % 360;
		} else {
			// back-facing
			result = (info.orientation - degrees + 360) % 360;
		}
		return result;
	}

	@Override
	public void onShutter() {
	}

	@Override
	public void onPictureTaken(byte[] data, Camera camera) {
		try {
			photoFile = getOutputMediaFile();
			if (photoFile == null) {
				Toast.makeText(activity,
						"Oops! could not save the photo.Try again",
						Toast.LENGTH_SHORT).show();
				return;
			}
			try {
				Bitmap realImage = BitmapFactory.decodeByteArray(data, 0,
						data.length);
				Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
				Camera.getCameraInfo(currentCameraId, info);
				bitmap = rotate(realImage, info.orientation);

				FileOutputStream fos = new FileOutputStream(photoFile);
				bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
				fos.close();

				bitmap = decodeFile(photoFile);
				showImageFragment(bitmap);
			} catch (Exception e) {
				Log.e(TAG, "Exception : " + e.getMessage());
			}
		} catch (Exception e) {
			Log.e(TAG, "onPictureTaken - wrote bytes: " + data.length);
		}
		camera.startPreview();
	}

	public static Bitmap rotate(Bitmap bitmap, int degree) {
		int w = bitmap.getWidth();
		int h = bitmap.getHeight();
		Matrix mtx = new Matrix();
		mtx.postRotate(degree);
		return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
	}

	private Bitmap decodeFile(File f) throws Exception {
		Bitmap bitmap = null;
		// Decode image size
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		FileInputStream fileInputStream = new FileInputStream(f);
		BitmapFactory.decodeStream(fileInputStream, null, options);
		fileInputStream.close();
		int scale = 1;
		if (options.outHeight > IMAGE_MAX_SIZE
				|| options.outWidth > IMAGE_MAX_SIZE) {
			// scale = (int) Math.pow(
			// 2,
			// (int) Math.ceil(Math.log(IMAGE_MAX_SIZE
			// / (double) Math.max(options.outHeight,
			// options.outWidth))
			// / Math.log(0.5)));
			scale = Math.max(
					Integer.highestOneBit((Math.max(options.outHeight,
							options.outWidth) - 1) / IMAGE_MAX_SIZE) << 1, 1);
		}
		// Decode with inSampleSize
		BitmapFactory.Options options2 = new BitmapFactory.Options();
		options2.inSampleSize = scale;
		fileInputStream = new FileInputStream(f);
		bitmap = BitmapFactory.decodeStream(fileInputStream, null, options2);
		fileInputStream.close();
		return bitmap;
	}

	private void showImageFragment(Bitmap bitmapImage) {
		MaterialDialog dialog = new MaterialDialog.Builder(this)
				.title(R.string.taken_photo)
				.customView(R.layout.fragment_view_photo, true)
				.positiveText(R.string.ok_photo)
				.negativeText(R.string.take_another)
				.callback(new MaterialDialog.ButtonCallback() {
					@Override
					public void onPositive(MaterialDialog dialog) {
						goNext();
					}

					@Override
					public void onNegative(MaterialDialog dialog) {
					}
				}).build();

		ImageView takenPhotoImageView = (ImageView) dialog.getCustomView()
				.findViewById(R.id.imageView_taken_photo);

		View positiveAction = dialog.getActionButton(DialogAction.POSITIVE);
		View negativeAction = dialog.getActionButton(DialogAction.NEGATIVE);

		positiveAction.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

			}
		});

		negativeAction.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

			}
		});

		if (bitmapImage != null) {
			Log.e(TAG, "Bitmap NOt null " + photoFile.getAbsolutePath());
			takenPhotoImageView.setImageBitmap(bitmapImage);
		} else {
			Log.e(TAG, "Bitmap null");
		}
		dialog.show();
	}

	protected void goNext() {
		Bundle bundle = new Bundle();
		bundle.putString("phone", phoneNumber);
		bundle.putString("code", countryCode);
		bundle.putString("firstName", firstName);
		bundle.putString("lastName", lastName);
		bundle.putString("otherName", otherName);
		bundle.putString("country", country);
		bundle.putStringArrayList("places", places);
		Intent nameIntent = new Intent(RegisterPhoto.this,
				RegisterSummary.class);
		nameIntent.putExtras(bundle);
		RegisterSummary.bitmap = bitmap;
		RegisterSummary.photoFile = photoFile;
		startActivity(nameIntent);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		Camera.CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);
		int rotation = this.getWindowManager().getDefaultDisplay()
				.getRotation();
		int degrees = 0;
		switch (rotation) {
		case Surface.ROTATION_0:
			degrees = 0;
			break; // Natural orientation
		case Surface.ROTATION_90:
			degrees = 90;
			break; // Landscape left
		case Surface.ROTATION_180:
			degrees = 180;
			break;// Upside down
		case Surface.ROTATION_270:
			degrees = 270;
			break;// Landscape right
		}
		int rotate = (info.orientation - degrees + 360) % 360;
		// Set the 'rotation' parameter
		Camera.Parameters params = mCamera.getParameters();
		params.setRotation(rotate);
		mCamera.setParameters(params);
		mCamera.setDisplayOrientation(90);
		mCamera.startPreview();
	}

	private boolean hasAFrontCamera() {
		int cameraCount = 0;
		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		cameraCount = Camera.getNumberOfCameras();
		for (int i = 0; i < cameraCount; i++) {
			Camera.getCameraInfo(i, cameraInfo);
			if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
				hasFrontCamera = true;
				break;
			}
		}
		return hasFrontCamera;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		try {
			mCamera.setPreviewDisplay(mPreview.getHolder());
		} catch (Exception e) {
			Log.e(TAG, "Exception " + e.getLocalizedMessage());
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.e(TAG, "Surface destroyed");
	}

	@Override
	public void onPause() {
		super.onPause();
		mCamera.stopPreview();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mCamera.release();
	}

	/**
	 * Used to return the camera File output.
	 * 
	 * @return
	 */
	private File getOutputMediaFile() {
		File mediaFile = null;
		// String errorMessage = "Oops! could not save the photo.Try again";
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// We can read and write the media
			// get the path to sdcard
			File sdcard = Environment.getExternalStorageDirectory();
			// to this path add a new directory path
			File mediaStorageDir = new File(sdcard.getAbsolutePath()
					+ File.separator + DIRECTORY_IMAGES + File.separator);
			// create this directory if not already created
			if (!mediaStorageDir.exists()) {
				if (!mediaStorageDir.mkdirs()) {
					Log.e(TAG, "Required media storage does not exist");
					// Toast.makeText(activity, errorMessage, Toast.LENGTH_LONG)
					// .show();
					return null;
				}
			}
			// Create a media file name
			String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
					.format(new Date());
			mediaFile = new File(mediaStorageDir.getPath() + File.separator
					+ "IMG_" + timeStamp + ".jpg");
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// We can only read the media
			// Toast.makeText(activity, errorMessage, Toast.LENGTH_LONG).show();
		} else {
			// Something else is wrong.We can neither read nor write
			// Toast.makeText(activity, errorMessage, Toast.LENGTH_LONG).show();
		}
		return mediaFile;
	}

	public int[] getScreenSize() {
		WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		DisplayMetrics metrics = new DisplayMetrics();
		display.getMetrics(metrics);
		int width = metrics.widthPixels;
		int height = metrics.heightPixels;
		return new int[] { width, height };
	}
	
	/**
	 * Show a an message
	 * 
	 * @param title
	 * @param message
	 */
	public void showErrorMessage(String title, String message) {
		if (bottomSheet == null || !bottomSheet.isShowing()) {
			bottomSheet = new BottomSheet.Builder(this,
					R.style.BottomSheet_StyleDialog)
					.icon(R.drawable.ic_action_warning).title(title)
					.sheet(1, message).build();
			bottomSheet.show();
		}
	}
}