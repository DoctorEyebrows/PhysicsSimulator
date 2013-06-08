package physicsSimulator;

import java.awt.*;

class Triangle extends Polygon {

    public Triangle(Vector offset, Vector side1, Vector side2, Brush parent) {
        this.offset = offset.clone(); //offset vector of the triangle's first point from the object's COM (will begin as relative to object's first point, gets updated when COM is calculated)
        shape = new Vector[3];
        shape[0] = (side1.clone());
        shape[1] = (side2.clone());
        shape[2] = (side1.plus(side2).mult(-1));
        this.parent = parent;
        this.length = 3;
    }

    public Vector centroid() {
        return shape[0].plus(shape[2].mult(-1)).mult(1 / 3.0).plus(offset); //average of all three points (relative to shape's origin)
    }

    public double area() {
        Vector A = shape[0];
        Vector B = shape[2].mult(-1);
        return 0.5 * Math.abs(A.x * B.y - B.x * A.y); //   1/2 * the determinant, apparently
    }

    public boolean contains(Vector point) {  //make sure point is relative the the triangle's first point! (since the triangle doesn't know where it is relative to the origin)
        Vector absA = parent.COM.plus(offset); //TODO: add private vars to cache values like this one for future calculations
        Vector[] pointRel ={point.minus(absA), point.minus(absA.plus(shape[0])), point.minus(absA.minus(shape[2]))};
        boolean sign = pointRel[0].leftOf(shape[0]);
        for(int i=1; i<3; i++){
            if(pointRel[i].leftOf(shape[i]) != sign)return false;
        }
        return true;
    }

    public void print() {
        System.out.printf("[%f,%f]:  [%f,%f], [%f,%f], [%f,%f]\n", offset.x, offset.y, shape[0].x, shape[0].y, shape[1].x, shape[1].y, shape[2].x, shape[2].y);
    }

    @Override
    public void draw(Graphics g, Vector objectCOM) {
        Vector pos = objectCOM.plus(offset);
        for (Vector v : shape) {
            v.draw(g, pos);
            pos.add(v);
        }
    }
}
