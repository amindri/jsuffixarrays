package org.jsuffixarrays;

import java.util.ArrayList;

import org.carrot2.util.collect.primitive.IntQueue;

/**
 * Suffix array traversal routines (emulating corresponding suffix tree traversals).
 */
public final class Traversals
{
    /**
     * Visitor interface for post-order traversal methods in {@link Traversals}.
     */
    public interface IPostOrderVisitor
    {
        /**
         * Visits a node in the (virtual) suffix tree, labeled with <code>length</code>
         * objects starting at <code>start</code> in the input sequence.
         * 
         * @param start The node label's starting offset in the input sequence.
         * @param length The node label's length (number of symbols).
         * @param leaf <code>true</code> if this node is a leaf.
         */
        public void visitNode(int start, int length, boolean leaf);
    }

    /**
     * Visitor interface for post-order traversal methods that compute an aggregated value
     * during the traversal.
     */
    public interface IPostOrderComputingVisitor<E>
    {
        /**
         * Aggregate two values into the result. The aggregation function should be
         * symmetric, that is: <code>value1 + value2 = value2 + value1</code>.
         */
        public E aggregate(E value1, E value2);

        /**
         * Compute the initial value for a leaf node.
         * 
         * @param saIndex Index of the leaf node in the suffix array.
         * @param symbolIndex The node label's starting offset in the input sequence.
         * @param length The node label's length (number of symbols).
         * @return Returns the initial function value for the leaf node.
         */
        public E leafValue(int saIndex, int symbolIndex, int length);

        /**
         * Visits a node in the (virtual) suffix tree, labeled with <code>length</code>
         * objects starting at <code>start</code> in the input sequence.
         * 
         * @param start The node label's starting offset in the input sequence.
         * @param length The node label's length (number of symbols).
         * @param leaf <code>true</code> if this node is a leaf.
         * @param value Aggregated value for all sub-nodes of the given node.
         */
        public void visitNode(int start, int length, boolean leaf, E value);
    }

    /**
     * <p>
     * Post-order traversal of all branching nodes in a suffix tree (emulated using a
     * suffix array and the LCP array). Post-order traversal is also called <i>bottom-up
     * traversal</i> that is child nodes are reported before parent nodes (and the root is
     * the last node to process).
     * <p>
     * The algorithm implemented here is from <i>Efficient Substring Traversal with Suffix
     * Arrays</i> by Toru Kasai, Hiroki Arimura and Setsuo Arikawa, Dept of Informatics,
     * Kyushu University, Japan.
     * 
     * @param sequenceLength Input sequence length for the suffix array and LCP array.
     * @param sa Suffix array.
     * @param lcp Corresponding LCP array for a given suffix array.
     * @param visitor Callback visitor.
     */
    public static void postorder(final int sequenceLength, int [] sa, int [] lcp,
        IPostOrderVisitor visitor)
    {
        assert sequenceLength <= sa.length && sequenceLength <= lcp.length : "Input sequence length larger than suffix array or the LCP.";

        final IntQueue stack = new IntQueue();

        // Push the stack bottom marker (sentinel).
        stack.push(-1, -1);

        // Process every leaf.
        int top_h;
        for (int i = 0; i <= sequenceLength; i++)
        {
            final int h = (sequenceLength == i ? -1 : lcp[i]);

            while (true)
            {
                top_h = stack.get(stack.size() - 1);
                if (top_h <= h) break;

                // Visit the node and remove it from the end of the stack.
                final int top_i = stack.get(stack.size() - 2);
                final boolean leaf = (top_i < 0);
                stack.pop(2);

                visitor.visitNode(sa[leaf ? -(top_i + 1) : top_i], top_h, leaf);
            }

            if (top_h < h)
            {
                stack.push(i, h);
            }

            if (i < sequenceLength)
            {
                // Mark leaf nodes in the stack.
                stack.push(-(i + 1), sequenceLength - sa[i]);
            }
        }
    }

    /**
     * <p>
     * Post-order traversal of all branching nodes in a suffix tree (emulated using a
     * suffix array and the LCP array). Post-order traversal is also called <i>bottom-up
     * traversal</i> that is child nodes are reported before parent nodes (and the root is
     * the last node to process).
     * <p>
     * The algorithm implemented here is from <i>Efficient Substring Traversal with Suffix
     * Arrays</i> by Toru Kasai, Hiroki Arimura and Setsuo Arikawa, Dept of Informatics,
     * Kyushu University, Japan.
     * 
     * @param sequenceLength Input sequence length for the suffix array and LCP array.
     * @param sa Suffix array.
     * @param lcp Corresponding LCP array for a given suffix array.
     * @param visitor Callback visitor computing aggregate values when traversing the
     *            tree.
     * @param epsilon "Zero" value (epsilon) for computations.
     */
    public static <E> void postorder(final int sequenceLength, int [] sa, int [] lcp,
        E epsilon, IPostOrderComputingVisitor<E> visitor)
    {
        assert sequenceLength <= sa.length && sequenceLength <= lcp.length : "Input sequence length larger than suffix array or the LCP.";

        final IntQueue stack = new IntQueue();
        final ArrayList<E> values = new ArrayList<E>();

        // Push the stack bottom marker (sentinel).
        stack.push(-1, -1);
        values.add(epsilon);

        // Process every leaf.
        int top_h;
        E top_c;
        for (int i = 0; i <= sequenceLength; i++)
        {
            final int h = (sequenceLength == i ? -1 : lcp[i]);
            E ci = epsilon;

            while (true)
            {
                top_h = stack.get(stack.size() - 1);
                if (top_h <= h) break;

                // Visit the node and remove it from the end of the stack.
                top_c = values.remove(values.size() - 1);
                final int top_i = stack.get(stack.size() - 2);
                final boolean leaf = (top_i < 0);
                stack.pop(2);

                ci = visitor.aggregate(top_c, ci);
                visitor.visitNode(sa[leaf ? -(top_i + 1) : top_i], top_h, leaf, ci);

                top_c = values.get(values.size() - 1);
            }

            if (top_h < h)
            {
                stack.push(i, h);
                values.add(ci);
            }
            else
            {
                assert top_h == h;
                final int index = values.size() - 1;
                values.set(index, visitor.aggregate(ci, values.get(index)));
            }

            if (i < sequenceLength)
            {
                // Mark leaf nodes in the stack.
                stack.push(-(i + 1), sequenceLength - sa[i]);
                values.add(visitor.leafValue(i, sa[i], sequenceLength - sa[i]));
            }
        }
    }
}
