package physicsSimulator;

import java.awt.*;

class Vector {

    double x, y;

    public Vector() {
    }

    public Vector(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vector(Vector parent) {
        this.x = parent.x;
        this.y = parent.y;
    }
    
    public Vector(Point p){
        if(p != null){
            this.x = p.x;
            this.y = p.y;}
        else {
            this.x = 0; this.y = 0;
        }
    }

    @Override
    public Vector clone() {
        return new Vector(x, y);
    }

    public void setTo(Vector v) {
        x = v.x;
        y = v.y;
    }

    public void add(Vector v) {
        x += v.x;
        y += v.y;
    }

    public Vector plus(Vector v) {
        Vector out = new Vector();
        out.x = x + v.x;
        out.y = y + v.y;
        return out;
    }

    public void sub(Vector v) {
        x -= v.x;
        y -= v.y;
    }

    public Vector minus(Vector v) {
        Vector out = new Vector();
        out.x = x - v.x;
        out.y = y - v.y;
        return out;
    }

    public void scale(double scalar) {
        x *= scalar;
        y *= scalar;
    }

    public Vector mult(double scalar) {
        Vector out = new Vector();
        out.x = x * scalar;
        out.y = y * scalar;
        return out;
    }

    public double len() {
        return Math.sqrt((x * x + y * y));
    }

    public double dot(Vector v) {
        return x * v.x + y * v.y;
    }

    public Vector rot90() {
        return new Vector(y, -x);
    } //returns the vector rotated 90 degrees anti-clockwise

    public Vector rotMinus90() {
        return new Vector(-y, x);
    } //returns the vector rotated 90 degrees anti-clockwise
    //note: these look the wrong way round because of the upside-down coordinate system. Maybe they should actually be named the other way round because of that. Oh well.

    public Vector rotTheta(double theta) { //returns the vector rotated through theta radians anti-clockwise
        Vector out = new Vector(x, y);
        out = out.mult(Math.cos(theta)).plus(out.rot90().mult(Math.sin(theta)));
        return out;
    }
    public void rotate(double angle) { //like rotTheta but modifies the vector in place
        angle = -angle; //necessary because of inverted y coordinates
        double xx = x;
        this.x = Math.cos(angle)*x - Math.sin(angle)*y;
        this.y = Math.cos(angle)*y + Math.sin(angle)*xx;
    }
    
    public boolean leftOf(Vector v){ //returns true if this vector is this vector is on the left of v, using cross product (i.e. v turns left to point to this)
        return (v.x*this.y >= this.x*v.y);
    }
    
    public double cross(Vector v){
        return -(this.x*v.y - v.x*this.y); //minus sign inserted because of inverted-y coordinate system
    }
    
    public double sinLength(Vector v){
        //returns the height of v above this (positive when v points left of this)
        return this.cross(v)/v.len();
    }
    
    public double cosLength(Vector v){
        //returns the length of the projection of v onto this (i.e. how far along this v goes)
        return this.dot(v)/v.len();
    }

    public void draw(Graphics g, Vector origin) {
        int ox = (int) origin.x;
        int oy = (int) origin.y;
        g.drawLine(ox, oy, ox + (int) x, oy + (int) y);
    }
}
