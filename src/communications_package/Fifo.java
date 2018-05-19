package communications_package;

public class Fifo<T>{

	private Object[] fifo_queue;
	
	// tracking variables
	private int fifo_capacity;
	private int fifo_size;
	private int fifo_head;
	private int fifo_tail;
	
	public Fifo(int capacity) {
		// set up fifo 
		fifo_queue = new Object[capacity];
		//
		fifo_capacity = capacity;
		fifo_size = 0;
		fifo_head = 0;
		fifo_tail = 0;
	}

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
	
	public boolean IsEmpty()
	{
		return (boolean)(0 == fifo_size);
	}
	
	public boolean IsFull()
	{
		return (boolean)(fifo_size == fifo_capacity);
	}
	
	public int getSize()
	{
		return fifo_size;
	}
}
