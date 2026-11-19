package local.muyu.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

/** All distances are blocks. Timeline values are game ticks. X/Z are world axes, even when the viewer turns. */
final class LotusAnimation {
    static final int PETALS_PER_LAYER = 8;
    static final float INNER_HEIGHT = 2.20F, OUTER_HEIGHT = 2.42F;
    static final int RISE_END = 8, INNER_CLOSE = 13, OUTER_LAG = 4, CLOSE_DURATION = 21;
    static final int DISSOLVE_START = 42, DISSOLVE_END = 55, END = 60;
    private static final double[] WIDTH = {.58,.72,.86,.98,1,1,.90,.79,.66,.50,.32,.14};
    private static final int[] PALETTE = {0x96601d,0xc58729,0xe7a73a,0xf3bd50,0xfbd276,0xffe39b,0xffefbc};
    private static final double[] FLUTES = {-1,-.86,-.13,0,.13,.86,1};
    private static final double[] SHADES = {.80,.98,1.17,1.12,1.02,.84};
    private static final double TAU = Math.PI * 2;
    private final PoseStack.Pose pose;
    private final VertexConsumer out;
    private final GoldenLotusRenderer.Frame frame;
    private final double time, lift;

    LotusAnimation(PoseStack.Pose pose, VertexConsumer out, GoldenLotusRenderer.Frame frame) {
        this.pose = pose;
        this.out = out;
        this.frame = frame;
        time = frame.age();
        lift = .025 + .15 * ease(time / RISE_END);
    }

    void draw() {
        seat();
        for (int layer = 0; layer < 2; layer++) {
            boolean outer = layer == 0;
            double close = ease((time - INNER_CLOSE - (outer ? OUTER_LAG : 0)) / CLOSE_DURATION);
            for (int i = 0; i < PETALS_PER_LAYER; i++) {
                double angle = -Math.toRadians(frame.yaw()) + TAU * (i + (outer ? .25 : .75)) / PETALS_PER_LAYER;
                new Petal(angle, outer, close, 2*i + layer).draw();
            }
        }
    }

    /** A ribbon has a gold inner face, an amber outer face and connected edges.
     * Each band ends on a pixel step, with a narrow raised vein along its center. */
    private final class Petal {
        final boolean outer;
        final double sx, cz, close, height, width, root, length;
        final int index;
        Petal(double angle, boolean outer, double close, int index) {
            this.outer = outer;
            this.close = close;
            this.index = index;
            sx = Math.sin(angle);
            cz = Math.cos(angle);
            height = outer ? OUTER_HEIGHT : INNER_HEIGHT;
            width = (outer ? .48 : .38) * (1 - .15*close);
            root = outer ? .42 : .26;
            length = outer ? 1.80 : 1.45;
        }
        record Section(double radius,double y,double sin,double cos,double vein) {}
        Section section(double s) {
            double budRadius = root * (1-s) + .035*s + (outer ? 1.04 : .81)*Math.sin(Math.PI*s);
            double dy = height*s;
            double dr = budRadius-root;
            double reach = mix(length*s, Math.hypot(dy, dr), close);
            double angle = mix((outer ? .035 : .105) + .12*s, Math.atan2(dy,dr), close);
            double sin = Math.sin(angle), cos = Math.cos(angle);
            return new Section(root+reach*cos, lift+(outer ? 0 : .047)+reach*sin, sin, cos, .073*Math.sin(Math.PI*s));
        }
        Vec3 point(Section band,double side,double underside) {
            double ridge = band.vein - .23*Math.abs(side) + underside;
            double radius = band.radius - ridge*band.sin;
            double y = band.y + ridge*band.cos;
            return new Vec3(radius*sx + side*cz, y, radius*cz - side*sx);
        }
        void draw() {
            double vanish = (time-DISSOLVE_START)/(DISSOLVE_END-DISSOLVE_START);
            double reveal = ease((time+1)/5);
            for (int row=0; row<WIDTH.length; row++) {
                double lo = row/(double)WIDTH.length, hi = (row+1)/(double)WIDTH.length;
                double remain = 1-ease((vanish - (1-hi)*.81 - (index%3)*.013)/.15);
                if (remain <= 0) continue;
                double w = width*WIDTH[row];
                double opacity = reveal*remain*(frame.firstPerson() ? .72 : .98);
                int color = PALETTE[(row*(PALETTE.length-1))/(WIDTH.length-1)];
                var lower = section(lo);
                var upper = section(hi);
                // Fixed width within each band gives the silhouette its visible pixel steps.
                for (int strip=0; strip<SHADES.length; strip++) {
                    double left = w*FLUTES[strip], right = w*FLUTES[strip+1];
                    int face = tint(color, SHADES[strip]*(outer ? .97 : 1.07));
                    quad(point(lower,left,0),point(upper,left,0),point(upper,right,0),point(lower,right,0),face,opacity);
                    quad(point(lower,left,-.026),point(lower,right,-.026),point(upper,right,-.026),point(upper,left,-.026),
                            tint(face,.94),opacity);
                }
                for (int sign : SIDES) {
                    double edge = sign*w;
                    Vec3 a=point(lower,edge,0), b=point(upper,edge,0), c=point(upper,edge,-.026), d=point(lower,edge,-.026);
                    if (sign>0) quad(a,b,c,d,tint(color,1.16),opacity);
                    else quad(d,c,b,a,tint(color,1.16),opacity);
                }
            }
            // Two released glints per petal, emitted at the advancing tip-to-root dissolve front.
            for (int j=0;j<2;j++) {
                double s = j==0 ? .84 : .36;
                double born = DISSOLVE_START+(1-s)*(DISSOLVE_END-DISSOLVE_START)*.81+(index%3)*.3;
                double t = (time-born)/8;
                if (t<0 || t>=1) continue;
                var origin = point(section(s), (index%2==0 ? .1 : -.1),0);
                var p = origin.add(sx*t*.17, t*.47, cz*t*.17);
                pixel(p,.029*(1-.45*t),1-t);
            }
        }
    }
    private static final int[] SIDES = {-1,1};
    private void seat() {
        double alpha = ease((time+1)/5)*(1-ease((time-DISSOLVE_END)/(END-DISSOLVE_END)));
        for (int i=0;i<24;i++) {
            double a=i*TAU/24, b=(i+1)*TAU/24;
            double r1 = .51 + .045*Math.cos(8*a), r2=.51+.045*Math.cos(8*b);
            quad(radial(a,.02,lift+.011),radial(a,r1,lift+.009),radial(b,r2,lift+.009),radial(b,.02,lift+.011),
                    i%3==0 ? 0xe8b44e : 0xc38a35,alpha);
            quad(radial(a,r1,lift+.009),radial(a,r1*.93,lift-.020),radial(b,r2*.93,lift-.020),radial(b,r2,lift+.009),
                    0xffda83,alpha);
        }
    }
    private static Vec3 radial(double a, double radius, double y) {
        return new Vec3(radius*Math.sin(a),y,radius*Math.cos(a));
    }
    private void pixel(Vec3 p,double r,double alpha) {
        Vec3 a=p.add(-r,-r,-r), b=p.add(r,-r,-r), c=p.add(r,r,-r), d=p.add(-r,r,-r);
        Vec3 e=p.add(-r,-r,r), f=p.add(r,-r,r), g=p.add(r,r,r), h=p.add(-r,r,r);
        quad(d,c,b,a,0xffcf64,alpha); quad(e,f,g,h,0xffe499,alpha);
        quad(a,e,h,d,0xfac354,alpha); quad(f,b,c,g,0xffdb7b,alpha);
        quad(h,g,c,d,0xffefbb,alpha); quad(a,b,f,e,0xd4a552,alpha);
    }
    private void quad(Vec3 a,Vec3 b,Vec3 c,Vec3 d,int color,double alpha) {
        vertex(a,color,alpha,0,0); vertex(b,color,alpha,1,0);
        vertex(c,color,alpha,1,1); vertex(d,color,alpha,0,1);
    }
    private void vertex(Vec3 p,int color,double alpha,float u,float v) {
        if (frame.firstPerson()) {
            var fromEye = frame.feetFromEye();
            var forward = frame.forward();
            double x = p.x + fromEye.x, y = p.y + fromEye.y, z = p.z + fromEye.z;
            // Soften only petals close to the aiming ray. Looking around still reveals the fixed world-space mesh.
            double dot = (x*forward.x + y*forward.y + z*forward.z)/Math.max(.001, Math.sqrt(x*x+y*y+z*z));
            alpha *= ease((.974-dot)/.09);
        }
        int argb = ((int)(255*alpha) << 24) | color;
        out.addVertex(pose,(float)p.x,(float)p.y,(float)p.z).setColor(argb).setUv(u,v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xf000f0).setNormal(pose,0,1,0);
    }
    private static int tint(int rgb,double factor) {
        return (Math.min(255,(int)((rgb>>16)*factor))<<16)
                | (Math.min(255,(int)(((rgb>>8)&255)*factor))<<8) | Math.min(255,(int)((rgb&255)*factor));
    }
    private static double mix(double a,double b,double t) { return a+(b-a)*t; }
    private static double ease(double t) { t=Math.clamp(t,0,1); return t*t*(3-2*t); }
}
