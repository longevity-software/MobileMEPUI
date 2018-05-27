package communications_package;

public class Fifo<T>{

	private Object[] fifo_queue;
	
	// tracking variables
	private int fifo_capacity;
	private int fifo_size;
	private int fifo_head;
	private int fifo_tail;

	// name: 	Fifo
	// desc: 	Fifo constructor
	public Fifo(int capacity) {
		// set up fifo 
		fifo_queue = new Object[capacity];
		//
		fifo_capacity = capacity;
		fifo_size = 0;
		fifo_head = 0;
		fifo_tail = 0;
	}

	// name: 	Add
	// desc: 	adds an element to the fifo if there is space
	public boolean Add(T element){
		//
		boolean add_success = false;
		//
		if(fifo_size < fifo_capacity)
		{
			// add this element to the fifo
			fifo_queue[fifo_head] = element;
			//
			fifo_size++;
			//
			if(++fifo_head == fifo_capacity)
			{
				fifo_head = 0;
			}
		}
		//
		return add_success;
	}

	// name: 	Remove
	// desc: 	removes an element from the fifo
	@SuppressWarnings("unchecked")
	public T Remove() {
		//
		T element = null;
		//
		if(fifo_size != 0)
		{
			element = (T)fifo_queue[fifo_tail];
			//
			fifo_size--;
			//
			if(++fifo_tail == fifo_capacity)
			{
				fifo_tail = 0;
			}
		}
		//
		return element;
	}

	// name: 	IsEmpty
	// desc: 	returns true is the size is 0 else it returns false
	public boolean IsEmpty()
	{
		return (boolean)(0 == fifo_size);
	}

	// name: 	IsFull
	// desc: 	returns true if the fifo is full else it returns false
	public boolean IsFull()
	{
		return (boolean)(fifo_size == fifo_capacity);
	}

	// name: 	getSize
	// desc: 	returns the size of the fifo
	public int getSize()
	{
		return fifo_size;
	}
}
