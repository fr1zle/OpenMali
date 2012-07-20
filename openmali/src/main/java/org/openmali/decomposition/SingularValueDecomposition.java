/**
 * Copyright (c) 2007-2009, OpenMaLi Project Group all rights reserved.
 * 
 * Portions based on the Sun's javax.vecmath interface, Copyright by Sun
 * Microsystems or Kenji Hiranabe's alternative GC-cheap implementation.
 * Many thanks to the developers.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * Neither the name of the 'OpenMaLi Project Group' nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) A
 * RISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE
 */
package org.openmali.decomposition;

import org.openmali.FastMath;
import org.openmali.vecmath2.MatrixMxNf;

/**
 * Singular Value Decomposition.
 * <p>
 * For an m-by-n matrix A with m >= n, the singular value decomposition is
 * an m-by-n orthogonal matrix U, an n-by-n diagonal matrix S, and
 * an n-by-n orthogonal matrix V so that A = U*S*V'.
 * </p>
 * <p>
 * The singular values, sigma[k] = S[k][k], are ordered so that
 * sigma[0] >= sigma[1] >= ... >= sigma[n-1].
 * </p>
 * <p>
 * The singular value decompostion always exists, so the constructor will
 * never fail.  The matrix condition number and the effective numerical
 * rank can be computed from this decomposition.
 * </p>
 * 
 * @author <a href="http://math.nist.gov/javanumerics/jama/">JAMA</a>
 */
public class SingularValueDecomposition
{
    /**
     * Arrays for internal storage of U and V.
     * 
     * @serial internal storage of U.
     * @serial internal storage of V.
     */
    private MatrixMxNf U, V;
    
    /**
     * Array for internal storage of singular values.
     * 
     * @serial internal storage of singular values.
     */
    private final float[] s;
    
    /**
     * Row and column dimensions.
     * 
     * @serial row dimension.
     * @serial column dimension.
     */
    private final int m, n;
    
    /**
     * Constructs the singular value decomposition.
     * 
     * @param M    Rectangular matrix
     */
    public SingularValueDecomposition( MatrixMxNf M )
    {
        // Derived from LINPACK code.
        // Initialize.
        MatrixMxNf A = new MatrixMxNf( M );
        
        this.m = M.getNumRows();
        this.n = M.getNumCols();
        
        /*
         * Apparently the failing cases are only a proper subset of (m < n), 
         * so let's not throw error.
         * Correct fix to come later?
         * if ( m < n )
         * {
         *     throw new IllegalArgumentException( "Jama SVD only works for m >= n" ) );
         * }
         */
        final int nu = Math.min( m, n );
        this.s = new float[ Math.min( m + 1, n ) ];
        this.U = new MatrixMxNf( m, nu );
        this.V = new MatrixMxNf( n, n );
        float[] e = new float[ n ];
        float[] work = new float[ m ];
        boolean wantU = true;
        boolean wantV = true;
        
        // Reduce A to bidiagonal form, storing the diagonal elements
        // in s and the super-diagonal elements in e.
        
        int nct = Math.min( m - 1, n );
        int nrt = Math.max( 0, Math.min( n - 2, m ) );
        for ( int k = 0; k < Math.max( nct, nrt ); k++ )
        {
            if ( k < nct )
            {
                /*
                 * Compute the transformation for the k-th column and
                 * place the k-th diagonal in s[ k ].
                 * Compute 2-norm of k-th column without under/overflow.
                 */
                s[ k ] = 0f;
                for ( int i = k; i < m; i++ )
                {
                    s[ k ] = FastMath.hypot( s[ k ], A.get( i, k ) );
                }
                
                if ( s[ k ] != 0.0 )
                {
                    if ( A.get( k, k ) < 0.0f )
                    {
                        s[ k ] = -s[ k ];
                    }
                    
                    for ( int i = k; i < m; i++ )
                    {
                        A.div( i, k, s[ k ] );
                    }
                    
                    A.add( k, k, 1.0f );
                }
                
                s[ k ] = -s[ k ];
            }
            
            for ( int j = k + 1; j < n; j++ )
            {
                if ( ( k < nct ) & ( s[ k ] != 0.0f ) )
                {
                    // Apply the transformation.
                    
                    float t = 0f;
                    for ( int i = k; i < m; i++ )
                    {
                        t += A.get( i, k ) * A.get( i, j );
                    }
                    t = -t / A.get( k, k );
                    for ( int i = k; i < m; i++ )
                    {
                        A.add( i, j, t * A.get( i, k ) );
                    }
                }
                
                // Place the k-th row of A into e for the
                // subsequent calculation of the row transformation.
                
                e[ j ] = A.get( k, j );
            }
            
            if ( wantU & ( k < nct ) )
            {
                // Place the transformation in U for subsequent back multiplication.
                
                for ( int i = k; i < m; i++ )
                {
                    U.set( i, k, A.get( i, k ) );
                }
            }
            
            if ( k < nrt )
            {
                // Compute the k-th row transformation and place the
                // k-th super-diagonal in e[k].
                // Compute 2-norm without under/overflow.
                e[ k ] = 0f;
                for ( int i = k + 1; i < n; i++ )
                {
                    e[ k ] = FastMath.hypot( e[ k ], e[ i ] );
                }
                
                if ( e[ k ] != 0.0f )
                {
                    if ( e[ k + 1 ] < 0.0f )
                    {
                        e[ k ] = -e[ k ];
                    }
                    
                    for ( int i = k + 1; i < n; i++ )
                    {
                        e[ i ] /= e[ k ];
                    }
                    
                    e[ k + 1 ] += 1.0f;
                }
                
                e[ k ] = -e[ k ];
                
                if ( ( k + 1 < m ) & ( e[ k ] != 0.0f ) )
                {
                    // Apply the transformation.
                    
                    for ( int i = k + 1; i < m; i++ )
                    {
                        work[ i ] = 0.0f;
                    }
                    
                    for ( int j = k + 1; j < n; j++ )
                    {
                        for ( int i = k + 1; i < m; i++ )
                        {
                            work[ i ] += e[ j ] * A.get( i, j );
                        }
                    }
                    
                    for ( int j = k + 1; j < n; j++ )
                    {
                        float t = -e[ j ] / e[ k + 1 ];
                        for ( int i = k + 1; i < m; i++ )
                        {
                            A.add( i, j, t * work[ i ] );
                        }
                    }
                }
                
                if ( wantV )
                {
                    // Place the transformation in V for subsequent
                    // back multiplication.
                    
                    for ( int i = k + 1; i < n; i++ )
                    {
                        V.set( i, k, e[ i ] );
                    }
                }
            }
        }
        
        // Set up the final bidiagonal matrix or order p.
        
        int p = Math.min( n, m + 1 );
        if ( nct < n )
        {
            s[ nct ] = A.get( nct, nct );
        }
        
        if ( m < p )
        {
            s[ p - 1 ] = 0.0f;
        }
        
        if ( nrt + 1 < p )
        {
            e[ nrt ] = A.get( nrt, p - 1 );
        }
        e[ p - 1 ] = 0.0f;
        
        // If required, generate U.
        
        if ( wantU )
        {
            for ( int j = nct; j < nu; j++ )
            {
                for ( int i = 0; i < m; i++ )
                {
                    U.set( i, j, 0.0f );
                }
                
                U.set( j, j, 1.0f );
            }
            
            for ( int k = nct - 1; k >= 0; k-- )
            {
                if ( s[ k ] != 0.0f )
                {
                    for ( int j = k + 1; j < nu; j++ )
                    {
                        float t = 0f;
                        for ( int i = k; i < m; i++ )
                        {
                            t += U.get( i, k ) * U.get( i, j );
                        }
                        t = -t / U.get( k, k );
                        for ( int i = k; i < m; i++ )
                        {
                            U.add( i, j, t * U.get( i, k ) );
                        }
                    }
                    
                    for ( int i = k; i < m; i++ )
                    {
                        U.sub( i, k, -U.get( i, k ) );
                    }
                    
                    U.set( k, k, 1.0f + U.get( k, k ) );
                    
                    for ( int i = 0; i < k - 1; i++ )
                    {
                        U.set( i, k, 0.0f );
                    }
                }
                else
                {
                    for ( int i = 0; i < m; i++ )
                    {
                        U.set( i, k, 0.0f );
                    }
                    
                    U.set( k, k, 1.0f );
                }
            }
        }
        
        // If required, generate V.
        
        if ( wantV )
        {
            for ( int k = n - 1; k >= 0; k-- )
            {
                if ( ( k < nrt ) & ( e[ k ] != 0.0f ) )
                {
                    for ( int j = k + 1; j < nu; j++ )
                    {
                        float t = 0f;
                        for ( int i = k + 1; i < n; i++ )
                        {
                            t += V.get( i, k ) * V.get( i, j );
                        }
                        
                        t = -t / V.get( k + 1, k );
                        
                        for ( int i = k + 1; i < n; i++ )
                        {
                            V.add( i, j, t * V.get( i, k ) );
                        }
                    }
                }
                
                for ( int i = 0; i < n; i++ )
                {
                    V.set( i, k, 0.0f );
                }
                
                V.set( k, k, 1.0f );
            }
        }
        
        // Main iteration loop for the singular values.
        
        final int pp = p - 1;
        int iter = 0;
        float eps = FastMath.pow( 2.0f, -52.0f );
        float tiny = FastMath.pow( 2.0f, -966.0f );
        while ( p > 0 )
        {
            int k, kase;
            
            // Here is where a test for too many iterations would go.
            
            // This section of the program inspects for
            // negligible elements in the s and e arrays.  On
            // completion the variables kase and k are set as follows.
            
            // kase = 1     if s(p) and e[k-1] are negligible and k<p
            // kase = 2     if s(k) is negligible and k<p
            // kase = 3     if e[k-1] is negligible, k<p, and
            //              s(k), ..., s(p) are not negligible (qr step).
            // kase = 4     if e(p-1) is negligible (convergence).
            
            for ( k = p - 2; k >= -1; k-- )
            {
                if ( k == -1 )
                {
                    break;
                }
                if ( Math.abs( e[ k ] ) <= tiny + eps * ( Math.abs( s[ k ] ) + Math.abs( s[ k + 1 ] ) ) )
                {
                    e[ k ] = 0.0f;
                    break;
                }
            }
            if ( k == p - 2 )
            {
                kase = 4;
            }
            else
            {
                int ks;
                for ( ks = p - 1; ks >= k; ks-- )
                {
                    if ( ks == k )
                    {
                        break;
                    }
                    float t = ( ks != p ? Math.abs( e[ ks ] ) : 0f ) + ( ks != k + 1 ? Math.abs( e[ ks - 1 ] ) : 0f );
                    if ( Math.abs( s[ ks ] ) <= tiny + eps * t )
                    {
                        s[ ks ] = 0.0f;
                        break;
                    }
                }
                if ( ks == k )
                {
                    kase = 3;
                }
                else if ( ks == p - 1 )
                {
                    kase = 1;
                }
                else
                {
                    kase = 2;
                    k = ks;
                }
            }
            k++;
            
            // Perform the task indicated by kase.
            
            switch ( kase )
            {
                // Deflate negligible s(p).
                
                case 1:
                {
                    float f = e[ p - 2 ];
                    e[ p - 2 ] = 0.0f;
                    for ( int j = p - 2; j >= k; j-- )
                    {
                        float t = FastMath.hypot( s[ j ], f );
                        float cs = s[ j ] / t;
                        float sn = f / t;
                        s[ j ] = t;
                        if ( j != k )
                        {
                            f = -sn * e[ j - 1 ];
                            e[ j - 1 ] = cs * e[ j - 1 ];
                        }
                        if ( wantV )
                        {
                            for ( int i = 0; i < n; i++ )
                            {
                                t = cs * V.get( i, j ) + sn * V.get( i, p - 1 );
                                V.set( i, p - 1, -sn * V.get( i, j ) + cs * V.get( i, p - 1 ) );
                                V.set( i, j, t );
                            }
                        }
                    }
                }
                    break;
                
                // Split at negligible s(k).
                
                case 2:
                {
                    float f = e[ k - 1 ];
                    e[ k - 1 ] = 0.0f;
                    for ( int j = k; j < p; j++ )
                    {
                        float t = FastMath.hypot( s[ j ], f );
                        float cs = s[ j ] / t;
                        float sn = f / t;
                        s[ j ] = t;
                        f = -sn * e[ j ];
                        e[ j ] = cs * e[ j ];
                        if ( wantU )
                        {
                            for ( int i = 0; i < m; i++ )
                            {
                                t = cs * U.get( i, j ) + sn * U.get( i, k - 1 );
                                U.set( i, k - 1, -sn * U.get( i, j ) + cs * U.get( i, k - 1 ) );
                                U.set( i, j, t );
                            }
                        }
                    }
                }
                    break;
                
                // Perform one qr step.
                
                case 3:
                {
                    
                    // Calculate the shift.
                    
                    float scale = Math.max( Math.max( Math.max( Math.max( Math.abs( s[ p - 1 ] ), Math.abs( s[ p - 2 ] ) ), Math.abs( e[ p - 2 ] ) ), Math.abs( s[ k ] ) ), Math.abs( e[ k ] ) );
                    float sp = s[ p - 1 ] / scale;
                    float spm1 = s[ p - 2 ] / scale;
                    float epm1 = e[ p - 2 ] / scale;
                    float sk = s[ k ] / scale;
                    float ek = e[ k ] / scale;
                    float b = ( ( spm1 + sp ) * ( spm1 - sp ) + epm1 * epm1 ) / 2.0f;
                    float c = ( sp * epm1 ) * ( sp * epm1 );
                    float shift = 0.0f;
                    if ( ( b != 0.0 ) | ( c != 0.0 ) )
                    {
                        shift = FastMath.sqrt( b * b + c );
                        if ( b < 0.0 )
                        {
                            shift = -shift;
                        }
                        shift = c / ( b + shift );
                    }
                    float f = ( sk + sp ) * ( sk - sp ) + shift;
                    float g = sk * ek;
                    
                    // Chase zeros.
                    
                    for ( int j = k; j < p - 1; j++ )
                    {
                        float t = FastMath.hypot( f, g );
                        float cs = f / t;
                        float sn = g / t;
                        if ( j != k )
                        {
                            e[ j - 1 ] = t;
                        }
                        f = cs * s[ j ] + sn * e[ j ];
                        e[ j ] = cs * e[ j ] - sn * s[ j ];
                        g = sn * s[ j + 1 ];
                        s[ j + 1 ] = cs * s[ j + 1 ];
                        if ( wantV )
                        {
                            for ( int i = 0; i < n; i++ )
                            {
                                t = cs * V.get( i, j ) + sn * V.get( i, j + 1 );
                                V.set( i, j + 1, -sn * V.get( i, j ) + cs * V.get( i, j + 1 ) );
                                V.set( i, j, t );
                            }
                        }
                        t = FastMath.hypot( f, g );
                        cs = f / t;
                        sn = g / t;
                        s[ j ] = t;
                        f = cs * e[ j ] + sn * s[ j + 1 ];
                        s[ j + 1 ] = -sn * e[ j ] + cs * s[ j + 1 ];
                        g = sn * e[ j + 1 ];
                        e[ j + 1 ] = cs * e[ j + 1 ];
                        if ( wantU && ( j < m - 1 ) )
                        {
                            for ( int i = 0; i < m; i++ )
                            {
                                t = cs * U.get( i, j ) + sn * U.get( i, j + 1 );
                                U.set( i, j + 1, -sn * U.get( i, j ) + cs * U.get( i, j + 1 ) );
                                U.set( i, j, t );
                            }
                        }
                    }
                    e[ p - 2 ] = f;
                    iter = iter + 1;
                }
                    break;
                
                // Convergence.
                
                case 4:
                {
                    
                    // Make the singular values positive.
                    
                    if ( s[ k ] <= 0.0f )
                    {
                        s[ k ] = ( s[ k ] < 0.0f ? -s[ k ] : 0.0f );
                        if ( wantV )
                        {
                            for ( int i = 0; i <= pp; i++ )
                            {
                                V.set( i, k, -V.get( i, k ) );
                            }
                        }
                    }
                    
                    // Order the singular values.
                    
                    while ( k < pp )
                    {
                        if ( s[ k ] >= s[ k + 1 ] )
                        {
                            break;
                        }
                        float t = s[ k ];
                        s[ k ] = s[ k + 1 ];
                        s[ k + 1 ] = t;
                        if ( wantV && ( k < n - 1 ) )
                        {
                            for ( int i = 0; i < n; i++ )
                            {
                                t = V.get( i, k + 1 );
                                V.set( i, k + 1, V.get( i, k ) );
                                V.set( i, k, t );
                            }
                        }
                        if ( wantU && ( k < m - 1 ) )
                        {
                            for ( int i = 0; i < m; i++ )
                            {
                                t = U.get( i, k + 1 );
                                U.set( i, k + 1, U.get( i, k ) );
                                U.set( i, k, t );
                            }
                        }
                        k++;
                    }
                    iter = 0;
                    p--;
                }
                    break;
            }
        }
    }
    
    /* ------------------------
       Public Methods
     * ------------------------ */

    /**
     * @return the left singular vectors.
     */
    public final MatrixMxNf getU()
    {
        MatrixMxNf result = new MatrixMxNf( m, Math.min( m + 1, n ) );
        result.set( U );
        
        return ( result );
    }
    
    /**
     * @return the right singular vectors.
     */
    public MatrixMxNf getV()
    {
        MatrixMxNf result = new MatrixMxNf( n, n );
        result.set( V );
        
        return ( result );
    }
    
    /**
     * Return the one-dimensional array of singular values
     * @return diagonal of S.
     */
    public float[] getSingularValues()
    {
        return ( s );
    }
    
    /**
     * @return the diagonal matrix of singular values.
     */
    public MatrixMxNf getS()
    {
        MatrixMxNf S = new MatrixMxNf( n, n );
        
        for ( int i = 0; i < n; i++ )
        {
            for ( int j = 0; j < n; j++ )
            {
                S.set( i, j, 0f );
            }
            
            S.set( i, i, this.s[ i ] );
        }
        
        return ( S );
    }
    
    /**
     * Two norm
     * @return     max(S)
     */
    public final float norm2()
    {
        return ( s[ 0 ] );
    }
    
    /**
     * Two norm condition number
     * @return     max(S)/min(S)
     */
    public final float cond()
    {
        return ( s[ 0 ] / s[ Math.min( m, n ) - 1 ] );
    }
    
    /**
     * Effective numerical matrix rank
     * @return     Number of nonnegligible singular values.
     */
    public int rank()
    {
        final float eps = FastMath.pow( 2.0f, -52.0f );
        final float tol = Math.max( m, n ) * s[ 0 ] * eps;
        
        int r = 0;
        
        for ( int i = 0; i < s.length; i++ )
        {
            if ( s[ i ] > tol )
            {
                r++;
            }
        }
        
        return ( r );
    }
}
