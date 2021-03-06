package android.transition;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Path;
import android.util.AttributeSet;
import com.android.internal.R;

public class ArcMotion extends PathMotion {
    private static final float DEFAULT_MAX_ANGLE_DEGREES = 70.0f;
    private static final float DEFAULT_MAX_TANGENT = ((float) Math.tan(Math.toRadians(35.0d)));
    private static final float DEFAULT_MIN_ANGLE_DEGREES = 0.0f;
    private float mMaximumAngle = DEFAULT_MAX_ANGLE_DEGREES;
    private float mMaximumTangent = DEFAULT_MAX_TANGENT;
    private float mMinimumHorizontalAngle = 0.0f;
    private float mMinimumHorizontalTangent = 0.0f;
    private float mMinimumVerticalAngle = 0.0f;
    private float mMinimumVerticalTangent = 0.0f;

    public ArcMotion(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ArcMotion);
        setMinimumVerticalAngle(a.getFloat(Float.MIN_VALUE, 0.0f));
        setMinimumHorizontalAngle(a.getFloat(0, 0.0f));
        setMaximumAngle(a.getFloat(2, DEFAULT_MAX_ANGLE_DEGREES));
        a.recycle();
    }

    public void setMinimumHorizontalAngle(float angleInDegrees) {
        this.mMinimumHorizontalAngle = angleInDegrees;
        this.mMinimumHorizontalTangent = toTangent(angleInDegrees);
    }

    public float getMinimumHorizontalAngle() {
        return this.mMinimumHorizontalAngle;
    }

    public void setMinimumVerticalAngle(float angleInDegrees) {
        this.mMinimumVerticalAngle = angleInDegrees;
        this.mMinimumVerticalTangent = toTangent(angleInDegrees);
    }

    public float getMinimumVerticalAngle() {
        return this.mMinimumVerticalAngle;
    }

    public void setMaximumAngle(float angleInDegrees) {
        this.mMaximumAngle = angleInDegrees;
        this.mMaximumTangent = toTangent(angleInDegrees);
    }

    public float getMaximumAngle() {
        return this.mMaximumAngle;
    }

    private static float toTangent(float arcInDegrees) {
        if (arcInDegrees >= 0.0f && arcInDegrees <= 90.0f) {
            return (float) Math.tan(Math.toRadians((double) (arcInDegrees / 2.0f)));
        }
        throw new IllegalArgumentException("Arc must be between 0 and 90 degrees");
    }

    /* JADX WARNING: Removed duplicated region for block: B:30:0x00bb  */
    /* JADX WARNING: Removed duplicated region for block: B:33:0x00c4  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public Path getPath(float startX, float startY, float endX, float endY) {
        float ex;
        float ey;
        float minimumArcDist2;
        float arcDistX;
        float arcDistY;
        float arcDist2;
        float maximumArcDist2;
        float newArcDistance2;
        float ex2;
        float ey2;
        float f = startX;
        float f2 = startY;
        Path path = new Path();
        path.moveTo(f, f2);
        float deltaX = endX - f;
        float deltaY = endY - f2;
        float h2 = (deltaX * deltaX) + (deltaY * deltaY);
        float dx = (f + endX) / 2.0f;
        float dy = (f2 + endY) / 2.0f;
        float midDist2 = h2 * 0.25f;
        float minimumArcDist22 = 0.0f;
        boolean isMovingUpwards = f2 > endY;
        float ey3;
        if (deltaY == 0.0f) {
            ex = dx;
            ey = ((Math.abs(deltaX) * 0.5f) * this.mMinimumHorizontalTangent) + dy;
        } else if (deltaX == 0.0f) {
            ex = ((Math.abs(deltaY) * 0.5f) * this.mMinimumVerticalTangent) + dx;
            ey = dy;
        } else if (Math.abs(deltaX) < Math.abs(deltaY)) {
            ex = Math.abs(h2 / (2.0f * deltaY));
            if (isMovingUpwards) {
                ey3 = endY + ex;
                ey = endX;
            } else {
                ey3 = f2 + ex;
                ey = f;
            }
            minimumArcDist2 = (this.mMinimumVerticalTangent * midDist2) * this.mMinimumVerticalTangent;
            ex = ey;
            ey = ey3;
            arcDistX = dx - ex;
            arcDistY = dy - ey;
            arcDist2 = (arcDistX * arcDistX) + (arcDistY * arcDistY);
            maximumArcDist2 = (this.mMaximumTangent * midDist2) * this.mMaximumTangent;
            minimumArcDist22 = 0.0f;
            if (arcDist2 == 0.0f && arcDist2 < minimumArcDist2) {
                minimumArcDist22 = minimumArcDist2;
            } else if (arcDist2 > maximumArcDist2) {
                minimumArcDist22 = maximumArcDist2;
            }
            newArcDistance2 = minimumArcDist22;
            if (newArcDistance2 != 0.0f) {
                float ratio = (float) Math.sqrt((double) (newArcDistance2 / arcDist2));
                ex = dx + ((ex - dx) * ratio);
                ey = dy + ((ey - dy) * ratio);
            }
            ex2 = ex;
            ey2 = ey;
            path.cubicTo((f + ex2) / 2.0f, (f2 + ey2) / 2.0f, (ex2 + endX) / 2.0f, (ey2 + endY) / 2.0f, endX, endY);
            return path;
        } else {
            float eDistX = h2 / (2.0f * deltaX);
            if (isMovingUpwards) {
                ex = f + eDistX;
                ey3 = f2;
            } else {
                ex = endX - eDistX;
                ey3 = endY;
            }
            ey = ey3;
            minimumArcDist22 = (this.mMinimumHorizontalTangent * midDist2) * this.mMinimumHorizontalTangent;
        }
        minimumArcDist2 = minimumArcDist22;
        arcDistX = dx - ex;
        arcDistY = dy - ey;
        arcDist2 = (arcDistX * arcDistX) + (arcDistY * arcDistY);
        maximumArcDist2 = (this.mMaximumTangent * midDist2) * this.mMaximumTangent;
        minimumArcDist22 = 0.0f;
        if (arcDist2 == 0.0f) {
        }
        if (arcDist2 > maximumArcDist2) {
        }
        newArcDistance2 = minimumArcDist22;
        if (newArcDistance2 != 0.0f) {
        }
        ex2 = ex;
        ey2 = ey;
        path.cubicTo((f + ex2) / 2.0f, (f2 + ey2) / 2.0f, (ex2 + endX) / 2.0f, (ey2 + endY) / 2.0f, endX, endY);
        return path;
    }
}
