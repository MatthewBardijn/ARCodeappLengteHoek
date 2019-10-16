package com.student.matthew.arcodeapp;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    private ArFragment arFragment;
    private ModelRenderable andyRenderable;

    public int count = 1;
    public Pose pose1;
    public Pose pose2;
    public Pose pose3;

    TextView dist;
    TextView ang1;
    TextView ang2;
    TextView ang3;
    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    // CompletableFuture requires api level 24
    // FutureReturnValueIgnored is not valid
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }

        setContentView(R.layout.activity_main);
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        dist = (TextView) findViewById(R.id.distanceText);
        ang1 = (TextView) findViewById(R.id.angle1Text);
        ang2 = (TextView) findViewById(R.id.angle2Text);
        ang3 = (TextView) findViewById(R.id.angle3Text);
        // When you build a Renderable, Sceneform loads its resources in the background while returning
        // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().
        ModelRenderable.builder()
                .setSource(this, R.raw.down_arrow_v1)
                .build()
                .thenAccept(renderable -> andyRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            Toast toast =
                                    Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });

        arFragment.setOnTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                    if (andyRenderable == null) {
                        return;
                    }

                    // Create the Anchor.
                    Anchor anchor = hitResult.createAnchor();
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(arFragment.getArSceneView().getScene());
                    if (count == 1){
                        pose1 = anchorNode.getAnchor().getPose();
                        count++;
                    }
                    else if (count == 2){
                        pose2 = anchorNode.getAnchor().getPose();
                        float distance = calcDistancebetweenPoses(pose1, pose2);

                        dist.setText("Distance between objects is " + distance);

                        Toast toast =
                                Toast.makeText(this, "Distance between objects is "+ distance, Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();

                        count++;
                    }
                    else if(count == 3){
                        pose3 =  anchorNode.getAnchor().getPose();
                        calcAnglesTriangle(pose1, pose2, pose3);
                    }

                    TransformableNode andy = new TransformableNode(arFragment.getTransformationSystem());
                    andy.setParent(anchorNode);
                    andy.setRenderable(andyRenderable);
                    andy.select();
                });
    }

    public float calcDistancebetweenPoses(Pose start, Pose end){
        float dx = start.tx() - end.tx();
        float dy = start.ty() - end.ty();
        float dz = start.tz() - end.tz();

        float distanceMeters = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
        return distanceMeters;
    }

    public void calcAnglesTriangle(Pose pose1, Pose pose2, Pose pose3){
        float dist1To2 = calcDistancebetweenPoses(pose1, pose2);
        float dist2To3 = calcDistancebetweenPoses(pose2, pose3);
        float dist3To1 = calcDistancebetweenPoses(pose3, pose1);

        double angle1 = Math.toDegrees((double)Math.acos(((dist1To2 * dist1To2) + (dist2To3 * dist2To3) - (dist3To1 * dist3To1)) / (2 * dist1To2 * dist2To3)));
        double angle2 = Math.toDegrees((double)Math.acos(((dist2To3 * dist2To3) + (dist3To1 * dist3To1) - (dist1To2 * dist1To2)) / (2 * dist2To3 * dist3To1)));
        double angle3 = Math.toDegrees((double)Math.acos(((dist3To1 * dist3To1) + (dist1To2 * dist1To2) - (dist2To3 * dist2To3)) / (2 * dist3To1 * dist1To2)));

        double total = angle1 + angle2 + angle3;
        Toast toast =
                Toast.makeText(this, "Anchor 1 angle = "+ angle1+", Anchor 2 angle = "+ angle2+", Anchor 3 angle = "+ angle3, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();

        ang1.setText("anchor 1 angle = "+angle1);
        ang2.setText("anchor 2 angle = "+angle2);
        ang3.setText("anchor 3 angle = "+angle3);
    }
    /**
     * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
     * on this device.
     *
     * <p>Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
     *
     * <p>Finishes the activity if Sceneform can not run
     */
    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }
}