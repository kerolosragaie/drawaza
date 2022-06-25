package com.kerollosragaie.drawaza.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.kerollosragaie.drawaza.R
import com.kerollosragaie.drawaza.databinding.ActivityProfileBinding


class ProfileActivity : AppCompatActivity() {
    private lateinit var binding:ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Glide.with(this)
            .load("https://avatars.githubusercontent.com/u/40574386?v=4")
            .placeholder(R.drawable.loading)
            .error(R.drawable.ic_warning)
            .into(binding.ivDevPp)

        binding.llDevEmail.setOnClickListener {
            val sendEmail = Intent(Intent.ACTION_SENDTO)
            sendEmail.data = Uri.parse("mailto:"+"kerolosragaie@gmail.com")
            startActivity(sendEmail)
        }

        binding.llDevCv.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://drive.google.com/file/d/12tX8Y_jopAuhP0rjyjAbkbq8pwRj-rlT/view"))
            startActivity(browserIntent)
        }

        binding.llDevLi.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.linkedin.com/in/kerollos-ragaie/"))
            startActivity(browserIntent)
        }

        binding.llDevGh.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/kerolosragaie"))
            startActivity(browserIntent)
        }
    }
}