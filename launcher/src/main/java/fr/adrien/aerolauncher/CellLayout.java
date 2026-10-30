package fr.adrien.aerolauncher;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/** A fixed grid: every child covers spanX × spanY cells starting at (cellX, cellY). */
class CellLayout extends ViewGroup {

    static final class LP extends ViewGroup.LayoutParams {
        int x, y, sx, sy;
        LP(int x, int y, int sx, int sy) {
            super(MATCH_PARENT, MATCH_PARENT);
            this.x = x; this.y = y; this.sx = sx; this.sy = sy;
        }
    }

    final int cols;
    final int rows;

    CellLayout(Context c, int cols, int rows) {
        super(c);
        this.cols = cols;
        this.rows = rows;
        setClipChildren(false);
        setClipToPadding(false);
    }

    float cellW() {
        return (getWidth() - getPaddingLeft() - getPaddingRight()) / (float) cols;
    }

    float cellH() {
        return (getHeight() - getPaddingTop() - getPaddingBottom()) / (float) rows;
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int w = MeasureSpec.getSize(wSpec);
        int h = MeasureSpec.getSize(hSpec);
        float cw = (w - getPaddingLeft() - getPaddingRight()) / (float) cols;
        float ch = (h - getPaddingTop() - getPaddingBottom()) / (float) rows;
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            LP lp = (LP) v.getLayoutParams();
            v.measure(MeasureSpec.makeMeasureSpec(Math.round(lp.sx * cw), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(Math.round(lp.sy * ch), MeasureSpec.EXACTLY));
        }
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        float cw = cellW(), ch = cellH();
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            LP lp = (LP) v.getLayoutParams();
            int left = getPaddingLeft() + Math.round(lp.x * cw);
            int top = getPaddingTop() + Math.round(lp.y * ch);
            v.layout(left, top, left + v.getMeasuredWidth(), top + v.getMeasuredHeight());
        }
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LP(0, 0, 1, 1);
    }

    @Override
    protected boolean checkLayoutParams(LayoutParams p) {
        return p instanceof LP;
    }

    boolean isFree(int x, int y, int sx, int sy, View ignore) {
        if (x < 0 || y < 0 || x + sx > cols || y + sy > rows) return false;
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            if (v == ignore) continue;
            LP lp = (LP) v.getLayoutParams();
            boolean overlap = x < lp.x + lp.sx && x + sx > lp.x && y < lp.y + lp.sy && y + sy > lp.y;
            if (overlap) return false;
        }
        return true;
    }

    /** First free spot, row by row; null when the page is full. */
    int[] findFree(int sx, int sy) {
        for (int y = 0; y <= rows - sy; y++)
            for (int x = 0; x <= cols - sx; x++)
                if (isFree(x, y, sx, sy, null)) return new int[]{x, y};
        return null;
    }

    int[] cellAt(float px, float py) {
        int x = (int) ((px - getPaddingLeft()) / cellW());
        int y = (int) ((py - getPaddingTop()) / cellH());
        return new int[]{Math.max(0, Math.min(cols - 1, x)), Math.max(0, Math.min(rows - 1, y))};
    }
}
