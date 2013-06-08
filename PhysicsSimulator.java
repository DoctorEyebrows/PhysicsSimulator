package PhysicsSimulator;

import java.util.Random;
import java.util.ArrayList;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

enum Prefabs {

    TRIANGLE, SQUARE, TRAPEZIUM
}

//remember: ORIGIN AT TOP-LEFT
class PhysicsSimulator {

    public static World world;
    public static Graphics g;
    public static String s;
    public static double gravity;

    public static void main(String[] args) throws InterruptedException //for some reason "throws InterruptedException" is necessary if you're to use Thread.sleep
    {
        s = "klsfjsklj";
        gravity = 100;
        world = new World();
        world.populate();
        //START MAIN LOOP
        double frameRate = 50;
        long frameDuration = 20000000; //(nanoseconds)
        long t0;
        while (true) {
            //calculate world t seconds from now:
            t0 = System.nanoTime();
            if(world.ticks < 0)continue;
            world.update(0.01); //TODO: make t a static variable of World
            //now draw it:
            world.draw();
            frameDuration = System.nanoTime() - t0;
            //nap time
            while (System.nanoTime() - t0 < 10000000) { //sleep for 10ms
                Thread.sleep(1);
            }
        }
    }
}

class Prop extends Brush //represents an extended object obeying the laws of classical mechanics
{//TODO: investigate nullPointerException due to declaring a variable which is already initialized in the superclass
    public Prop(Vector origin) {
        super();
        createShape(origin);
        rotate(Math.random()*Math.PI*2);
        vel = new Vector(0, 0);
        angVel = 0;
        oneOverMass = 1.0/10;
        oneOverI = 1.0/10000;
        elasticity = 0.3;
        //offsetImpulse(new Vector(0,20),new Vector(20,0));
        PhysicsSimulator.world.registerObject(this); //note: "this" here is a reference to a prop, while "this" in Brush() is a reference to a brush. That's why I don't call super.
    }

    @Override
    public void drift(double t) {
        //moves the object to its new position t seconds later, assuming its velocity doesn't change in that time period
        COM.add(vel.mult(t));
        rotate(angVel*t);
    }
    
    @Override
    public void impulse(Vector p) {
        //adds impulse p to the prop's momentum, as though a brief force had acted on it during this frame
        vel.add(p.mult(oneOverMass));
    }
    
    public void offsetImpulse(Vector offset, Vector p) {
        //like impulse but takes the force's point of action (relative to this.COM) into account, resulting in a change in angVel
        impulse(p);
        angVel += (offset.cross(p) * oneOverI);
    }
    
    public void applyGravity(double t) {
        //accelerates the object a bit, corresponding to gravity
        vel.y += PhysicsSimulator.gravity * t;
    }
    
    @Override
    public void update(double t)
    {
        //props however, do do shit
        drift(t);
        //applyGravity(t); //acceleration due to gravitational field; g = 9.8 pixels/second^2 //can't do this until world has handled collisions
    }
}

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

class World {

    private JFrame frame;
    private MyPanel contentPane;
    private ArrayList<Brush> objects;
    private ArrayList<Brush> brushes;
    private ArrayList<Prop> props;
    
    static public ArrayList<DrawLine> drawLines;
    
    public ArrayList<Brush> intersectors;
    
    int ticks;

    public World() {
        objects = new ArrayList<Brush>();
        brushes = new ArrayList<Brush>();
        props = new ArrayList<Prop>();
        drawLines = new ArrayList<DrawLine>();
        intersectors = new ArrayList<Brush>();
        ticks = 0;

        //Set up the JFrame (that's a window)
        frame = new JFrame("Another physics simulator"); //TODO: put all this shit in a separate display class, it probably doesn't belong here
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 400);
        frame.setBackground(Color.BLACK);

        contentPane = new MyPanel();
        frame.setContentPane(contentPane);

        frame.setVisible(true);
        System.out.println("Done");
    }

    public void populate() {
        //Set up the world
        new Brush(new Vector(200, 200));
        Prop p = new Prop(new Vector(200, 40));
        //p.angVel = 0.8;
        /*p.rotate(Math.PI);
        Prop p = new Prop(new Vector(50,100));
        p.vel.x = 50;
        p.vel.y = -25;*/
    }
    
    public void restart() {
        objects.clear();
        props.clear();
        brushes.clear();
        drawLines.clear();
        populate();
        ticks = 0;
    }

    public void update(double t) {
        try{
        System.out.println("Tick "+ticks+":");
        Vector r = new Vector(contentPane.getMousePosition());
        
        for (Brush obj : objects) {
            //if(r != null) prop.COM = r.clone(); //move prop to mouse position
            obj.update(t);
            ///////////////////////////testing contains method /////////////////////////:
            if(obj.enclosesPoint(r)){drawLines.add(new DrawDot(new Vector(40,20),Color.pink)); break;} /////////////////////////
        }
        
        intersectors.clear();
        System.out.println(intersectors.size());
        box_intersections();
        System.out.println(intersectors.size());
        for(int i=0; i<intersectors.size(); i+=2){
            System.out.println("Boxes are intersecting!");
            Brush a = intersectors.get(i);
            Brush b = intersectors.get(i+1);
            if (a.SAT(b))
            {
                System.out.println("Intersection");
                //now do SCIENCE
                handleCollision(a,b,t);
            }
            else
                System.out.println("No intersection");
        }
        
        for (Prop obj : props) {
            obj.applyGravity(t);
        }
        
        ticks++;
        }
        catch (java.util.ConcurrentModificationException e) {System.out.println("Warning: swing's being a bitch!");}
    }
    
    public void handleCollision(Brush a, Brush b, double t)
    {
        //binary search the objects back in time until they're just touching
        a.drift(-t/2); b.drift(-t/2);
        t /= 2;
        int iterations=1;
        while(true){
            if(a.SAT(b)){if(iterations>=5)break; a.drift(-t/2); b.drift(-t/2);}
            else
            {a.drift(t/2); b.drift(t/2);}
            iterations++;
            t /= 2;
        }
        //Objects are now close enough to where they were at the moment of collision
        //Now figure out exactly where the collision occurred, and make sure that a is the object with the colliding face, and b wtih the colliding point:
        Brush swapVar = null;
        Vector pos = a.COM.plus(a.polygon.offset);
        for(int i=0; i<a.polygon.length; i++){
            if(b.enclosesPoint(pos)){swapVar=a; a=b; b=swapVar; break;} //a point of a is in b, so a has the colliding point and b has the colliding face
            pos.add(a.polygon.shape[i]);
        }
        if(swapVar == null){ //the colliding point must be in b, and a must have the colliding face, so no swap is needed, but we still need to find the collision point
            pos = b.COM.plus(b.polygon.offset);
            for(int i=0; i<b.polygon.length; i++){
                if(a.enclosesPoint(pos))break;
                pos.add(b.polygon.shape[i]);
            }
        }
        //pos should now be the absolute position of the collision
        //now we have to determine which side of which shape the point collided with
        double minDist = 10000;
        double sinLength;
        double cosLength;
        Vector corner = a.COM.plus(a.polygon.offset);
        Vector s = corner; //corner used as a placeholder, because java needs to know the variable gets initialized
        Vector cornerToP;
        Vector collSide = corner; //side involved in the collision (closest to the colliding point). As with s, corner is an arbitrary placeholder
        for(int i=0; i<a.polygon.length; i++){
            s = a.polygon.shape[i];
            cornerToP = pos.minus(corner);
            sinLength = Math.abs(s.sinLength(cornerToP));
            cosLength = s.cosLength(cornerToP);
            if(cosLength > 0 && cosLength < s.len() && sinLength < minDist){
                minDist = sinLength;
                collSide = s;
            }
            corner.add(s);
        }
        //s should now be the side which is involved in the collision
        Vector n = collSide.rot90();
        World.drawLines.add(new DrawDot(a.COM,Color.white));
        //now apply an offset force to both objects, acting at point pos, in direction n, with magnitude sufficient to prevent objects clipping and dependent on object elasticities
        double e = a.elasticity*b.elasticity;
        Vector r_a = pos.minus(a.COM);
        Vector r_b = pos.minus(b.COM);
        Vector u_a = a.vel.plus(r_a.rot90().mult(a.angVel));
        Vector u_b = b.vel.plus(r_b.rot90().mult(b.angVel));
        

        double numerator = u_a.minus(u_b).dot(n);
        if(numerator > 0.01)numerator *= (1+e); //effectively setting e to 0 when the objects are colliding very slowly, i.e. during resting contact
        double denominator = (n.dot(n)*(a.oneOverMass+b.oneOverMass));
        double gammaB = r_b.rot90().mult(b.oneOverI*r_b.cross(n)).dot(n);
        double intermediate = a.oneOverI*r_a.cross(n);
        double gammaA = r_a.rot90().mult(intermediate).dot(n);
        denominator += gammaA + gammaB;
        double lambda = numerator / denominator;
        System.out.println(numerator);
        System.out.println(denominator);
        System.out.println(lambda);
        n.scale(lambda);
        World.drawLines.add(new DrawLine(pos,n,Color.GREEN));
        a.offsetImpulse(r_a,n.mult(-1)); b.offsetImpulse(r_b,n);
        //a.impulse(n.mult(-1)); b.impulse(n);
        a.drift(0.01-t); b.drift(0.01-t);
        //ticks = -2;
    }

    public void box_intersections() {
        //returns a list of pairs of objects whose bounding boxes are intersecting
        for (int i = 0; i < (objects.size() - 1); i++) {
            for (int j = i + 1; j < objects.size(); j++) {
                Brush obji = objects.get(i);
                Brush objj = objects.get(j);
                if(obji.box_intersection(objj)){ //obji and objj's bounding boxes overlap*/ 
                    intersectors.add(obji);
                    intersectors.add(objj);
                }
            }
        }
    }

    public void registerObject(Brush brush) {
        objects.add(brush);
        brushes.add(brush);
    }

    public void registerObject(Prop prop) {
        objects.add(prop);
        props.add(prop);
    }

    public void draw() {
        frame.repaint();
    }

    class MyPanel extends JPanel implements MouseListener, KeyListener{

        private Vector brushOrigin;

        public MyPanel()
        {
            super();
            addMouseListener(this);
            addKeyListener(this);
            setFocusable(true);
        }
        
        public void mousePressed(MouseEvent mouse)
        {
            World.drawLines.add(new DrawDot(new Vector(40,40),Color.white));
        }
        
        public void mouseEntered(MouseEvent mouse){ } ////////////// ////////////////////////////////////////// 
        public void mouseExited(MouseEvent mouse){ }  //Necessary to implement MouseListener interface,
        public void mouseClicked(MouseEvent mouse){ } //but not used.
        public void mouseReleased(MouseEvent mouse){ }/////////////////////////////////////////////////////////
        
        public void keyTyped(KeyEvent key){ }
        public void keyReleased(KeyEvent key){ }
        
        public void keyPressed(KeyEvent key)
        {
            if(key.getKeyCode() == KeyEvent.VK_R){
                restart();
            }
        }
        
        @Override
        public void paintComponent(Graphics g) {
            //draw the brushes
            for (Brush brush : objects) {
                g.setColor(Color.WHITE);
                Vector COM = brush.COM;
                if (brush.drawTriangles) {
                    for (Triangle tri : brush.triangles) {
                        tri.draw(g, COM);
                    }
                }  {
                    g.setColor(Color.BLUE);
                    brush.polygon.draw(g, COM);
                }
                if (brush.drawCOM) {
                    g.setColor(Color.BLUE);
                    g.drawLine((int) COM.x, (int) COM.y, (int) COM.x, (int) COM.y); //draw a dot on the COM
                }
                double[] box = brush.boundingBox;
                if (brush.drawBoundingBox) {
                    g.setColor(Color.RED);
                    g.drawRect((int) (COM.x + box[0]), (int) (COM.y + box[2]), (int) (box[1] - box[0]), (int) (box[3] - box[2]));
                    //boundingBox really should be a class rather than an array
                }
            }
            for(DrawLine line : drawLines)line.draw(g);
            drawLines.clear();
        }
    }
}

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

class DrawLine {
    //utility class, exists to be drawn once in the next draw call and then discarded

    Vector v1, v2;
    boolean endpoints = false; //true if v2 defines the endpoint of the line, false if v2 defines the line to draw from the start point (v1)
    Color colour;

    public DrawLine(Vector v1, Vector v2) {
        this.v1 = v1;
        this.v2 = v2;
        endpoints = false;
    }

    public DrawLine(Vector v1, Vector v2, boolean endpoints) {
        this.v1 = v1;
        this.v2 = v2;
        this.endpoints = endpoints;
    }
    
    public DrawLine(Vector v1, Vector v2,Color colour) {
        this.v1 = v1;
        this.v2 = v2;
        endpoints = false;
        this.colour = colour;
    }

    public DrawLine(Vector v1, Vector v2, boolean endpoints, Color colour) {
        this.v1 = v1;
        this.v2 = v2;
        this.endpoints = endpoints;
        this.colour = colour;
    }
    
    public DrawLine(){} //for implicit calls from DrawDot

    public void draw(Graphics g) {
        if(colour != null)g.setColor(colour);
        if (endpoints) {
            v2.minus(v1).draw(g, v1);
        } else {
            v2.draw(g, v1);
        }
        g.setColor(Color.red);
    }
}

class DrawDot extends DrawLine //only really extended for polymorphism
{
    Vector v;
    
    public DrawDot(Vector v)
    {
        this.v = v;
    }
    
    public DrawDot(Vector v,Color c)
    {
        this.v = v;
        colour = c;
    }
    
    @Override
    public void draw(Graphics g)
    {
        if(colour != null)g.setColor(colour);
        g.fillOval((int)v.x-2,(int)v.y-2,4,4);
        g.setColor(Color.red);
    }
}