package physicsSimulator;

import java.util.Random;
import java.util.ArrayList;

class Brush //represents an extended body that just sits there and doesn't move (like the floor)
{ //TODO: make this into a general class, and subclass Brush and Prop from it. A brush should just be a Prop with infinite mass

    Vector COM; //absolute position of the object's centre of mass
    Vector offset; //vector from COM to polygon's first point
    Vector vel;
    double[] boundingBox;
    ArrayList<Triangle> triangles;
    Polygon polygon;
    boolean drawTriangles;
    boolean drawBoundingBox;
    boolean drawCOM;
    double elasticity;
    double oneOverMass;
    double angVel;
    double oneOverI;

    public static Vector[] Prefab(Prefabs type) {
        Vector[] shape;
        switch (type) {
            case TRIANGLE:
                shape = new Vector[3];
                shape[0] = new Vector(4, -4);
                shape[1] = new Vector(4, 4);
                shape[2] = new Vector(-8, 0);
                break;
            case SQUARE:
                shape = new Vector[4];
                shape[0] = new Vector(0, -4);
                shape[1] = new Vector(4, 0);
                shape[2] = new Vector(0, 4);
                shape[3] = new Vector(-4, 0);
                break;
            case TRAPEZIUM:
                shape = new Vector[4];
                shape[0] = new Vector(3, -3);
                shape[1] = new Vector(3, 0);
                shape[2] = new Vector(3, 3);
                shape[3] = new Vector(-9, 0);
                break;
            default:
                shape = new Vector[3];
                shape[0] = new Vector(4, -4);
                shape[1] = new Vector(4, 4);
                shape[2] = new Vector(-8, 0);
                break;
        }
        return shape;
    }

    public Brush(Vector origin) {
        createShape(origin);
        //rotate(Math.random()*Math.PI*2);
        elasticity = 1;
        vel = new Vector(0,0);
        oneOverMass = 0;
        oneOverI = 0;
        angVel = 0;
        PhysicsSimulator.world.registerObject(this);
    }

    public Brush() {
    } //necessary because implicit super() call inserted in Prop's constructor. Truely, java never misses an opportunity to fuck with me.

    public void createShape(Vector origin) {
        //sets up an object's physical form, location and bounding box (general for brushes and props; does not include physical properties)
        Random rand = new Random();
        this.COM = origin;
        //for now, all brushes are either triangles, squares or trapeziums
        int type = rand.nextInt(3);
        int length;
        if (type == 0) {
            length = 3;
        } else {
            length = 4;
        }
        Vector[] shape = new Vector[length];
        switch (type) {
            case 0: //triangle
                System.out.println("triangle");
                shape = Prefab(Prefabs.TRIANGLE);
                break;
            case 1: //square
                System.out.println("square");
                shape = Prefab(Prefabs.SQUARE);
                break;
            case 2: //trapezium
                System.out.println("trapezium");
                shape = Prefab(Prefabs.TRAPEZIUM);
                break;
        }
        polygon = new Polygon(shape,this); //use this for drawing the brush
        polygon.scale(10);
        triangles = triangleDecomposition();
        computeCOM();
        boundingBox = computeBoundingBox();
        drawTriangles = true;
        drawBoundingBox = false;
        drawCOM = true;
    }

    public void scale(double factor) {
        //scales the brush about its origin
        polygon.scale(factor);
        for (Triangle tri : triangles) {
            tri.offset.scale(factor); //move it in or out from the COM (since we're scaling about the COM)
            for (int i = 0; i < 3; i++) {
                tri.shape[i].scale(factor); //make it bigger or smaller
            }
        }
        for (int i = 0; i < 4; i++) {
            boundingBox[i] *= factor;
        }
    }

    public double[] computeBoundingBox() { //TODO: make this a method of polygon that returns a bounding box class
        //returns a list in the form [minX,maxX,minY,maxY], relative to COM
        double maxX = 0;
        double maxY = 0;
        double minX = 0;
        double minY = 0;
        Vector point = offset.clone();
        for (Vector v : polygon.shape) {
            if (point.x > maxX) {
                maxX = point.x;
            } else if (point.x < minX) {
                minX = point.x;
            }
            if (point.y > maxY) {
                maxY = point.y;
            } else if (point.y < minY) {
                minY = point.y;
            }
            point.add(v);
        }
        double[] box = new double[4];
        box[0] = minX;
        box[1] = maxX;
        box[2] = minY;
        box[3] = maxY;
        return box;
    }

    public void computeCOM() {
        //returns the position of the object's centre of mass, relative to its origin
        //do so by averaging the COMs of each of the object's component triangles:
        offset = new Vector(0, 0); //vector from polygon's first point to COM
        double area;
        double totalArea = 0;
        for (Triangle triangle : triangles) {
            area = triangle.area();
            offset.add(triangle.centroid().mult(area));
            totalArea += area;
        }
        offset.scale(1 / totalArea);
        offset.scale(-1); //now it points from the COM to the first point - more useful

        for (Triangle tri : triangles) {
            tri.offset.add(offset);
        }
        polygon.setOffset(offset);

    }

    public ArrayList<Triangle> triangleDecomposition() {
        ArrayList<Vector> clipped = new ArrayList<Vector>(polygon.length);
        for (int i = 0; i < polygon.length; i++) {
            clipped.add(polygon.shape[i].clone());
        }
        ArrayList<Triangle> triangles = new ArrayList<Triangle>();
        Vector pos = new Vector(0, 0);
        int i = 0;

        //now run around the shape and clip its ears off:
        while (clipped.size() > 3) {
            i = 0;
            while (i < clipped.size() - 1 && clipped.size() > 3) {
                Vector perpAxis = clipped.get(i).plus(clipped.get(i + 1)).rot90();
                if (clipped.get(i).dot(perpAxis) > 0) {//not a notch
                    //so clip the ear off
                    triangles.add(new Triangle(pos, clipped.get(i), clipped.get(i + 1),this));
                    clipped.get(i).add(clipped.get(i + 1));
                    clipped.remove(i + 1);
                }
                pos.add(clipped.get(i));
                i++;
            }
        }
        //clipped is now just a triangle; add it to the list
        triangles.add(new Triangle(pos, clipped.get(i), clipped.get(i + 1),this));
        return triangles;
    }
    
    public boolean box_intersection(Brush target) {
    //returns true iff this and target's bounding boxes are intersecting
    double i1, i2, j1, j2;
    //check for overlap along x-axis:
    i1 = boundingBox[0] + COM.x; //min x
    i2 = boundingBox[1] + COM.x; //max x
    j1 = target.boundingBox[0] + target.COM.x; //min x
    j2 = target.boundingBox[1] + target.COM.x; //max x
    if (i2 >= j1 && j2 >= i1) {
        //found overlap along x-axis, now check y-axis:
        i1 = boundingBox[2] + COM.y; //min y
        i2 = boundingBox[3] + COM.y; //max y
        j1 = target.boundingBox[2] + target.COM.y; //min y
        j2 = target.boundingBox[3] + target.COM.y; //max y
        if (i2 >= j1 && j2 >= i1)/*obji and objj's bounding boxes overlap*/
            return true;
    }
    return false;
    }
    
    public boolean SAT(Brush brush)
    {
        if(polygon.SAT(brush.polygon))return true;
        else return false;
    }
    
    public ArrayList<Brush> collisions(ArrayList<Brush> objects)
    {
        ArrayList<Brush> colliders = new ArrayList<Brush>();
        for(Brush obj : objects){
            if(box_intersection(obj)){
                if(SAT(obj))colliders.add(obj);
            }
        }
        return colliders;
    }
    
    public void impulse(Vector p) {
        
    }
    
    public void offsetImpulse(Vector offset, Vector p) {
        //like impulse but takes the force's point of action (relative to this.COM) into account, resulting in a change in angVel
        impulse(p);
        angVel += (offset.cross(p) * oneOverI);
    }
    
    public void update(double t)
    {
        //brushes don't do shit
        //rotate(0.01);
    }
    
    public void drift(double t){} //need this; see World.collisionHandler
    
    public void rotate(double theta){
        polygon.rotate(theta);
        offset.rotate(theta);
        for(Triangle tri : triangles){
            tri.rotate(theta);
        }
        boundingBox = computeBoundingBox();
    }
    
    public boolean enclosesPoint(Vector p)
    {
        for(Triangle tri : triangles){
            if(tri.contains(p))return true;
        }
        return false;
    }
}

