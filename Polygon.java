package physicsSimulator;

import java.awt.*;

class Polygon {
//exists mostly for drawing purposes

    Vector offset;
    Vector[] shape;
    int length;
    Brush parent;

    public Polygon() {
    } //exists only to be called implicitly from Triangle()

    public Polygon(Vector offset, Vector[] shape, Brush parent) {
        this.offset = offset.clone(); //vector from the object's COM to the polygon's first point
        this.shape = shape;
        length = shape.length;
        this.parent = parent;
    }

    public Polygon(Vector[] shape, Brush parent) { //exists because offset isn't necessarily known at point of creation
        this.shape = shape;
        length = shape.length;
        this.parent = parent;
    }

    public void setOffset(Vector offset) {
        this.offset = offset.clone();
    }

    public void scale(double factor) {
        //scales the brush about its parent object's COM
        for (int i = 0; i < shape.length; i++) {
            shape[i].scale(factor);
        }
    }
    
    public void rotate(double angle){
        offset.rotate(angle);
        for(int i=0; i<length; i++){
            shape[i].rotate(angle);
        }
    }

    public boolean SAT(Polygon poly) {
        //FOR NOW, THIS METHOD ASSUMES THAT BOTH POLYGONS ARE CONVEX
        double s2, px, py, t;
        Vector COM = parent.COM;
        int n = shape.length + poly.shape.length; //total number of points from both polygons (also total number of sides)
        Vector axis;
        double maxA, minA, maxB, minB; //projection boundaries for the two polygons (where A = this, B = poly)


        Vector[] points = new Vector[n];
        points[0] = offset;
        for (int j = 1; j < shape.length; j++) {
            points[j] = points[j - 1].plus(shape[j - 1]);
        }
        points[shape.length] = poly.parent.COM.minus(COM);
        points[shape.length].add(poly.offset);
        for (int j = shape.length + 1; j < n; j++) {
            points[j] = points[j - 1].plus(poly.shape[j - shape.length - 1]);
        }
        //points should now be an array of vectors from this polygon's COM to all the points in the two polygons

        int i = 0; //index of current side
        while (i<n) {
            if(i<shape.length)
                axis = shape[i].rot90();
            else
                axis = poly.shape[i-shape.length].rot90();
            //World.drawLines.add(new DrawLine(COM.plus(points[i]),axis.clone(),Color.magenta));
            s2 = axis.x * axis.x + axis.y * axis.y;
            maxA = -1000;
            minA = 1000;
            maxB = -1000;
            minB = 1000;
            for (int j = 0; j < n; j++) {
                t = axis.dot(points[j].minus(points[i])) / s2;
                if (j < shape.length) {
                    if (t > maxA)
                        maxA = t;
                    if (t < minA)
                        minA = t;
                } else {
                    if (t > maxB) 
                        maxB = t;
                    if (t < minB) 
                        minB = t;
                }
            }
            //now check for separation along the axis
            /*World.drawLines.add(new DrawDot(COM.plus(points[i]).plus(axis.mult(maxA)),Color.blue));
            World.drawLines.add(new DrawDot(COM.plus(points[i]).plus(axis.mult(minA)),Color.cyan));
            World.drawLines.add(new DrawDot(COM.plus(points[i]).plus(axis.mult(maxB)),Color.red));
            World.drawLines.add(new DrawDot(COM.plus(points[i]).plus(axis.mult(minB)),Color.orange));*/
            if (!(maxA >= minB && maxB >= minA)) {
                return false;
            }
            i++;

        }
        World.drawLines.add(new DrawDot(new Vector(20,20),Color.green));
        return true;
    }

    public void draw(Graphics g, Vector pos) { //expects pos to be the absolute position off the object's COM
        pos = pos.plus(offset);
        for (Vector v : shape) {
            v.draw(g, pos);
            pos.add(v);
        }
    }
}
