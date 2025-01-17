package com.example.modernaandroiduppgift2final

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.modernaandroiduppgift2final.ui.theme.ModernaAndroidUppgift2FinalTheme
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable


// Post data class with dynamic imageUrl
data class Post(
    val id: Int,
    val title: String,
    val body: String,
    val imageUrl: String // Dynamically set image URL
)

interface PostApi {
    @GET("posts")
    fun getPosts(): Call<List<Post>>
}

object RetrofitInstance {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://jsonplaceholder.typicode.com/") // Base URL for the API
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: PostApi = retrofit.create(PostApi::class.java)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ModernaAndroidUppgift2FinalTheme {
                PostListScreen()
            }
        }
    }
}

@Composable
fun PostListScreen() {
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var selectedPost by remember { mutableStateOf<Post?>(null) } // Track selected post
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var buttonText by remember { mutableStateOf("Fetch Posts") }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        if (selectedPost == null) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(onClick = {
                    isLoading = true
                    buttonText = "Fetching..."
                    fetchPosts(
                        onPostsFetched = { fetchedPosts ->
                            posts = fetchedPosts // Replace old posts with new ones
                            isLoading = false
                            buttonText = "Fetch New Posts"
                        },
                        onError = { error ->
                            errorMessage = error
                            isLoading = false
                            buttonText = "Fetch Posts"
                        }
                    )
                }) {
                    Text(text = buttonText)
                }

                if (isLoading) {
                    CircularProgressIndicator()
                }

                errorMessage?.let {
                    val context = LocalContext.current
                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    items(posts, key = { post -> post.id }) { post ->
                        PostItem(post = post, onPostClick = { selectedPost = it })
                    }
                }
            }
        } else {
            SinglePostView(
                post = selectedPost!!,
                onEditClick = { /* Handle Edit Action */ },
                onDeleteClick = {
                    posts = posts.filter { it.id != selectedPost!!.id } // Remove post
                    selectedPost = null // Go back to post list
                },
                onBackClick = { selectedPost = null }
            )
        }
    }
}

@Composable
fun PostItem(post: Post, onPostClick: (Post) -> Unit) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .clickable { onPostClick(post) }
    ) {
        Text(text = post.title, style = MaterialTheme.typography.titleMedium, color = Color.Black)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = post.body, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        Image(
            painter = rememberAsyncImagePainter(post.imageUrl),
            contentDescription = "Post Image",
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
fun SinglePostView(
    post: Post,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onBackClick) {
            Text("Back to Posts")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = post.title, style = MaterialTheme.typography.titleMedium, color = Color.Black)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = post.body, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        Image(
            painter = rememberAsyncImagePainter(post.imageUrl),
            contentDescription = "Post Image",
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(bottom = 16.dp)
        )
        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onEditClick) {
                Text("Edit")
            }
            Button(onClick = onDeleteClick) {
                Text("Delete")
            }
        }
    }
}

// Modify fetchPosts to ensure unique posts
fun fetchPosts(onPostsFetched: (List<Post>) -> Unit, onError: (String) -> Unit) {
    RetrofitInstance.api.getPosts().enqueue(object : Callback<List<Post>> {
        override fun onResponse(call: Call<List<Post>>, response: Response<List<Post>>) {
            if (response.isSuccessful) {
                val posts = response.body()?.take(5)?.map { post ->
                    post.copy(
                        title = shuffleWords(post.title), // Shuffle words in the title
                        body = modifyBody(post.body), // Slightly modify the body content
                        imageUrl = "https://picsum.photos/600/200?random=${System.currentTimeMillis() + post.id}" // Unique image
                    )
                } ?: emptyList()

                onPostsFetched(posts)
            } else {
                onError("Failed to fetch posts: ${response.message()}")
            }
        }

        override fun onFailure(call: Call<List<Post>>, t: Throwable) {
            onError("Failed to fetch posts: ${t.message}")
        }
    })
}

/**
 * Shuffles words in a string to create a dynamically modified title.
 */
fun shuffleWords(text: String): String {
    val words = text.split(" ").toMutableList()
    words.shuffle() // Randomize word order
    return words.joinToString(" ")
}

/**
 * Modifies the body by splitting it into sentences, reversing one, or adding variations.
 */
fun modifyBody(body: String): String {
    val sentences = body.split(". ").toMutableList()
    if (sentences.size > 1) {
        val randomIndex = (sentences.indices).random() // Pick a random sentence
        sentences[randomIndex] = sentences[randomIndex].reversed() // Reverse a random sentence
    }
    return sentences.joinToString(". ") + " (Updated)"
}




@Preview(showBackground = true)
@Composable
fun PostListScreenPreview() {
    ModernaAndroidUppgift2FinalTheme {
        PostListScreen()
    }
}
