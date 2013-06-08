package physicsSimulator;

import java.util.Random;
import java.util.ArrayList;
import java.awt.*;
import javax.swing.*;

enum Prefabs {TRIANGLE,SQUARE,TRAPEZIUM}

//remember: ORIGIN AT TOP-LEFT
class PhysicsSimulator
{
	public static World world;
	public static Graphics g;
	public static ArrayList<Vector> drawQueue;
	public static void main(String[] args) throws InterruptedException //for some reason "throws InterruptedException" is necessary if you're to use Thread.sleep
	{
		world = new World();
		world.populate();
		drawQueue = new ArrayList<Vector>();
		//START MAIN LOOP
		double frameRate = 50;
		long frameDuration = 20000000; //(nanoseconds)
		long t0;
		while(true){
			//calculate world t seconds from now:
			t0 = System.nanoTime();
			world.update(0.01);
			//now draw it:
			world.draw();
			frameDuration = System.nanoTime() - t0;
			//nap time
			while(System.nanoTime()-t0 < 10000000)Thread.sleep(1); //sleep for 10ms
			}
	}
	
	
}

class Prop extends Brush
//represents an extended object obeying the laws of classical mechanics
{
	Vector vel;
	double mass;
	
	public Prop(Vector origin)
	{
		createShape(origin);
		vel = new Vector(0,0);
		mass = 10;
		PhysicsSimulator.world.registerObject(this); //note: "this" here is a reference to a prop, while "this" in Brush() is a reference to a brush. That's why I don't call super.
	}
		
	public void drift(double t){
		//moves the object to its new position t seconds later, assuming its velocity doesn't change in that time period
		COM.add(vel.mult(t));
	}
	
	public void impulse(Vector p){
		//adds impulse p to the prop's momentum, as though a brief force had acted on it during this frame
		vel.add(p.mult(1/mass));
	}
}

class Brush
//represents an extended body that just sits there and doesn't move (like the floor)
{
	Vector COM; //absolute position of the object's centre of mass
	Vector offset; //vector from COM to polygon's first point
	double[] boundingBox;
	ArrayList<Triangle> triangles;
	Polygon polygon;
	boolean drawTriangles;
	boolean drawBoundingBox;
	boolean drawCOM;
	
	public static Vector[] Prefab(Prefabs type){
	Vector[] shape;
	switch(type) {
	case TRIANGLE: 
	shape = new Vector[3]; shape[0] = new Vector(4,-4); shape[1] = new Vector(4,4); shape[2] = new Vector(-8,0); break;
	case SQUARE:
	shape = new Vector[4]; shape[0] = new Vector(0,-4); shape[1] = new Vector(4,0); shape[2] = new Vector(0,4); shape[3] = new Vector(-4,0); break;
	case TRAPEZIUM:
	shape = new Vector[4]; shape[0] = new Vector(3,-3); shape[1] = new Vector(3,0); shape[2] = new Vector(3,3); shape[3] = new Vector(-9,0); break;
	default:
	shape = new Vector[3]; shape[0] = new Vector(4,-4); shape[1] = new Vector(4,4); shape[2] = new Vector(-8,0); break;
	}
	return shape;
	}
	
	public Brush(Vector origin){
		createShape(origin);
		PhysicsSimulator.world.registerObject(this);
	}
	
	public Brush(){} //necessary because implicit super() call inserted in Prop's constructor. Truely, java never misses an opportunity to fuck with me.
	
	public void createShape(Vector origin){
		//sets up an object's physical form, location and bounding box (general for brushes and props; does not include physical properties)
		Random rand = new Random();
		this.COM = origin;
		//for now, all brushes are either triangles, squares or trapeziums
		int type = rand.nextInt(3);
		int length;
		if(type == 0) length = 3;
		else length = 4;
		Vector[] shape = new Vector[length];
		switch(type) {
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
		polygon = new Polygon(shape); //use this for drawing the brush
		polygon.scale(10);
		triangles = triangleDecomposition();
		computeCOM();
		boundingBox = computeBoundingBox();
		drawTriangles = true;
		drawBoundingBox = true;
		drawCOM = true;
	}
	
	public void scale(double factor) {
	//scales the brush about its origin
	for(Triangle tri : triangles) {
		tri.offset.scale(factor); //move it in or out from the COM (since we're scaling about the COM)
		for(int i=0; i<3; i++)tri.shape[i].scale(factor); //make it bigger or smaller
	}
	for(int i=0; i<4; i++)boundingBox[i] *= factor;
	}
	
	public double[] computeBoundingBox(){
		//returns a list in the form [minX,maxX,minY,maxY], relative to COM
		double maxX = 0;
		double maxY = 0;
		double minX = 0;
		double minY = 0;
		Vector point = offset.clone();
		for(Vector v : polygon.shape){
			if(point.x > maxX)maxX = point.x;
			else if (point.x < minX)minX = point.x;
			if(point.y > maxY)maxY = point.y;
			else if (point.y < minY)minY = point.y;
			point.add(v);
		}
		double[] box = new double[4];
		box[0]=minX; box[1]=maxX; box[2]=minY; box[3]=maxY;
		return box;
	}	

	public void computeCOM(){
		//returns the position of the object's centre of mass, relative to its origin
		//do so by averaging the COMs of each of the object's component triangles:
		offset = new Vector(0,0); //vector from polygon's first point to COM
		double area;
		double totalArea = 0;
		for(Triangle triangle : triangles){
			area = triangle.area();
			offset.add(triangle.centroid().mult(area));
			totalArea += area;
		}
		offset.scale(1/totalArea);
		offset.scale(-1); //now it points from the COM to the first point - more useful
		
		for(Triangle tri : triangles)tri.offset.add(offset);
		polygon.setOffset(offset);
		
	}
	
	public ArrayList<Triangle> triangleDecomposition(){
		ArrayList<Vector> clipped = new ArrayList<Vector>(polygon.length);
		for(int i=0; i<polygon.length; i++)clipped.add(polygon.shape[i].clone());
		ArrayList<Triangle> triangles = new ArrayList<Triangle>();
		Vector pos = new Vector(0,0);
		int i = 0;
		
		//now run around the shape and clip its ears off:
		while(clipped.size()>3){
			i = 0;
			System.out.println(i);
			System.out.println(clipped);
			while(i<clipped.size()-1 && clipped.size()>3){
				Vector perpAxis = clipped.get(i).plus(clipped.get(i+1)).rot90();
				if(clipped.get(i).dot(perpAxis) > 0){//not a notch
					//so clip the ear off
					System.out.println("CLIP");
					triangles.add(new Triangle(pos,clipped.get(i),clipped.get(i+1)));
					clipped.get(i).add(clipped.get(i+1));
					clipped.remove(i+1);
				}
				pos.add(clipped.get(i));
				i++;
			}
		}
		//clipped is now just a triangle; add it to the list
		triangles.add(new Triangle(pos,clipped.get(i),clipped.get(i+1)));
		return triangles;
	}
}

class Triangle extends Polygon
{
	Vector offset;
	Vector[] shape;
	Vector[] containerAxes;
	
	public Triangle(Vector offset, Vector side1, Vector side2){
		this.offset = offset.clone(); //offset vector from the object's COM (will begin as relative to object's first point, gets updated when COM is calculated)
		shape = new Vector[3];
		shape[0] = (side1.clone());
		shape[1] = (side2.clone());
		shape[2] = (side1.plus(side2).mult(-1));
		containerAxes = new Vector[3]; //axes to be used for finding if a point lies in the triangle
		containerAxes[0] = side1.rot90(); containerAxes[1] = side2.rot90(); containerAxes[2] = shape[2].rot90();
		}
		
	public Vector centroid(){
	return shape[0].plus(shape[2].mult(-1)).mult(1/3.0).plus(offset); //average of all three points (relative to shape's origin)
	}
	
	public double area(){
		Vector A = shape[0];
		Vector B = shape[2].mult(-1);
		return 0.5*Math.abs(A.x*B.y - B.x*A.y); //   1/2 * the determinant, apparently
	}
	
	public Boolean contains(Vector point){
	return false;
	}
		
	public void print(){
	System.out.printf("[%f,%f]:  [%f,%f], [%f,%f], [%f,%f]\n",offset.x,offset.y,shape[0].x,shape[0].y,shape[1].x,shape[1].y,shape[2].x,shape[2].y);
	}
	
	public void draw(Graphics g,Vector objectCOM){
		Vector pos = objectCOM.plus(offset);
		for(Vector v : shape){
			v.draw(g,pos);
			pos.add(v);
		}
	}
}

class Polygon
{
//exists mostly for drawing purposes
	Vector offset;
	Vector[] shape;
	int length;
	
	public Polygon(){} //exists only to be called implicitly from Triangle()
	
	public Polygon(Vector offset,Vector[] shape){
		this.offset = offset; //vector from the object's COM to the polygon's first point
		this.shape = shape;
		length = shape.length;
	}
	
	public Polygon(Vector[] shape){
		this.shape = shape;
		length = shape.length;
	}
	
	public void setOffset(Vector offset){
		this.offset = offset;
	}
	
	public void scale(double factor) {
	//scales the brush about its parent object's COM
	for(int i=0; i<shape.length;i++) {
		shape[i].scale(factor);
	}
	}
	
	public boolean SAT(Vector COM, Vector polyCOM, Polygon poly) {
		//FOR NOW, THIS METHOD ASSUMES THAT BOTH POLYGONS ARE CONVEX
		double s2, px, py, t;
		int n = shape.length+poly.shape.length; //total number of points from both polygons (also total number of sides)
		Vector axis;
		double maxA, minA, maxB, minB; //projection boundaries for the two polygons (where A = this, B = poly)
		maxA=0; minA=0; maxB=0; minB=0;
		
		
		Vector[] points = new Vector[n];
		points[0] = offset;
		for(int j=1; j<shape.length; j++)points[j] = points[j-1].plus(shape[j-1]);
		points[shape.length] = polyCOM.minus(COM);
		points[shape.length].add(poly.offset);
		for(int j=shape.length+1; j<shape.length+poly.shape.length; j++)points[j] = points[j-1].plus(poly.shape[j-shape.length-1]);
		//points should now be an array of vectors from this polygon's COM to all the points in the two polygons
		
		Vector[] sides = shape; //pointer to current side array, allows for iterating through both polygons' sides without copying them all into a new array or list
		int i = 0; //index of current side
		while(true)
		{
			axis = sides[i].rot90();
			s2 = axis.x*axis.x + axis.y*axis.y;
			for(int j=0; j<n; j++){
				t = axis.dot(points[j].minus(points[i])) / s2;
				if(j<shape.length){
					if(t>maxA)maxA=t;
					else if(t<minA)minA=t;}
				else {
					if(t>maxB)maxB=t;
					else if(t<minB)minB=t;}
			}
			//now check for separation along the axis
			if(!(maxA >= minB && maxB >= minA))return false;
			i++;
			if(sides==shape){
				if(i==shape.length){sides=poly.shape; i=0;}
			} else
				{ if(i==poly.shape.length)break; }
		}
		return true;
	}
	public void draw(Graphics g,Vector pos){ //expects pos to be the absolute position off the object's COM
		pos = pos.plus(offset);
		for(Vector v : shape){
			v.draw(g,pos);
			pos.add(v);
		}
	}
}

class World
{
	private JFrame frame;
	private MyPanel contentPane;
	public ArrayList<Brush> objects;
	public ArrayList<Brush> brushes;
	public ArrayList<Prop> props;
	
	public World(){
		objects = new ArrayList<Brush>();
		brushes = new ArrayList<Brush>();
		props = new ArrayList<Prop>();
		
		//Set up the JFrame (that's a window)
		frame = new JFrame("Another physics simulator");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(400,400);
		frame.setBackground(Color.BLACK);
		
		contentPane = new MyPanel();
		frame.setContentPane(contentPane);
		
		frame.setVisible(true);
		System.out.println("Done");
	}
	
	public void populate(){
		//Set up the world
		Brush a = new Brush(new Vector(200,250));
		Prop b = new Prop(new Vector(200,40));
	}
	
	public void update(double t){
		for(Prop prop : props){
			prop.drift(t);
			prop.vel.y += 9.8*t; //acceleration due to gravitational field; g = 9.8 pixels/second^2
		}
		if(objects.get(0).polygon.SAT(objects.get(0).COM,objects.get(1).COM,objects.get(1).polygon))
		System.out.println("Intersection");
		else
		System.out.println("No intersection");
	}
	
	public ArrayList<Brush> box_intersections(){
		//returns a list of pairs of objects whose bounding boxes are intersecting
		double i1, i2, j1, j2;
		ArrayList<Brush> intersectors = new ArrayList<Brush>();
		for(int i=0; i<(objects.size()-1); i++){
			for(int j=i+1; j<objects.size(); j++){
				Brush obji = objects.get(i);
				Brush objj = objects.get(j);
				//check for overlap along x-axis:
				i1 = obji.boundingBox[0]+obji.COM.x; //min x
				i2 = obji.boundingBox[1]+obji.COM.x; //max x
				j1 = objj.boundingBox[0]+objj.COM.x; //min x
				j2 = objj.boundingBox[1]+objj.COM.x; //max x
				if(i2 >= j1 && j2 >= i1){
					//found overlap along x-axis, now check y-axis:
					i1 = obji.boundingBox[2]+obji.COM.y; //min y
					i2 = obji.boundingBox[3]+obji.COM.y; //max y
					j1 = objj.boundingBox[2]+objj.COM.y; //min y
					j2 = objj.boundingBox[3]+objj.COM.y; //max y
					if(i2 >= j1 && j2 >= i1)/*obji and objj's bounding boxes overlap*/{intersectors.add(obji); intersectors.add(objj);}}
				}
			}
		return intersectors;
	}
	
	public void registerObject(Brush brush){
	objects.add(brush);
	brushes.add(brush);
	}
		
	public void registerObject(Prop prop){
	objects.add(prop);
	props.add(prop);
	}
	
	public void draw(){
		frame.repaint();
	}
	
	class MyPanel extends JPanel{
		private Vector brushOrigin;
		
		public void paintComponent(Graphics g){
			//draw the brushes
			for(Brush brush : objects){
				g.setColor(Color.WHITE);
				Vector COM = brush.COM;
				if(brush.drawTriangles)
					for(Triangle tri : brush.triangles)tri.draw(g,COM);
				else
					brush.polygon.draw(g,COM);
				if(brush.drawCOM){
					g.setColor(Color.BLUE);
					g.drawLine((int)COM.x,(int)COM.y,(int)COM.x,(int)COM.y);
				}
				double[] box = brush.boundingBox;
				if(brush.drawBoundingBox)
					g.setColor(Color.RED);
					g.drawRect((int)(COM.x+box[0]),(int)(COM.y+box[2]),(int)(box[1]-box[0]),(int)(box[3]-box[2])); 
					//boundingBox really should be a class rather than an array
			}
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
	
	public Vector clone(){return new Vector(x,y);}
	
	public void setTo(Vector v){x = v.x; y = v.y;}
	
	public void add(Vector v) {
	x += v.x;
	y += v.y;
	}
	
	public Vector plus(Vector v) {
	Vector out = new Vector();
	out.x = x+v.x;
	out.y = y+v.y;
	return out;
	}
	
	public void sub(Vector v) {
	x -= v.x;
	y -= v.y;
	}
	
	public Vector minus(Vector v) {
	Vector out = new Vector();
	out.x = x-v.x;
	out.y = y-v.y;
	return out;
	}
	
	public void scale(double scalar) {
	x *= scalar;
	y *= scalar;
	}
	
	public Vector mult(double scalar) {
	Vector out = new Vector();
	out.x = x*scalar;
	out.y = y*scalar;
	return out;
	}
	
	public double len() {
	return Math.sqrt((x*x + y*y));
	}
	
	public double dot(Vector v) {
	return x*v.x + y*v.y;
	}
	
	public Vector rot90(){return new Vector(y,-x);} //returns the vector rotated 90 degrees anti-clockwise
	
	public Vector rotMinus90(){return new Vector(-y,x);} //returns the vector rotated 90 degrees anti-clockwise
	//note: these look the wrong way round because of the upside-down coordinate system. Maybe they should actually be named the other way round because of that. Oh well.
	
	public Vector rotTheta(double theta){ //returns the vector rotated through theta radians anti-clockwise
	Vector out = new Vector(x,y);
	out = out.mult(Math.cos(theta)).plus(out.rot90().mult(Math.sin(theta)));
	return out;
	}
	
	public void draw(Graphics g, Vector origin) {
	int ox = (int)origin.x;
	int oy = (int)origin.y;
	g.drawLine(ox,oy,ox+(int)x,oy+(int)y);
	}
}
