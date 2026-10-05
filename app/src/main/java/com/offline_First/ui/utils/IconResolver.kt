package com.offline_First.ui.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Centralized icon resolver for EduNova.
 * Maps semantic backend icon keys to Material Design icons.
 * 
 * Usage:
 *   Icon(imageVector = IconResolver.resolve(course.icon), ...)
 *   Icon(imageVector = IconResolver.resolve(roadmap.icon), ...)
 */
object IconResolver {
    
    /**
     * Resolve semantic icon key to Material icon.
     * 
     * @param iconKey Semantic key from backend (e.g., "python", "data_science")
     * @return Material icon, or fallback icon if key is unknown
     */
    fun resolve(iconKey: String?): ImageVector {
        return when (iconKey?.lowercase()?.trim()) {
            // Programming
            "python" -> Icons.Default.Code
            "java" -> Icons.Default.Code
            "javascript" -> Icons.Default.Code
            
            // Data & AI
            "data_science" -> Icons.Default.Analytics
            "machine_learning" -> Icons.Default.Psychology
            "deep_learning" -> Icons.Default.Memory
            "artificial_intelligence" -> Icons.Default.Psychology
            "generative_ai" -> Icons.Default.AutoAwesome
            
            // Development
            "android" -> Icons.Default.Android
            "web_development" -> Icons.Default.Language
            "full_stack" -> Icons.Default.Layers
            "mobile_development" -> Icons.Default.PhoneAndroid
            
            // Computer Science
            "algorithms" -> Icons.Default.Functions
            "database" -> Icons.Default.Storage
            "operating_systems" -> Icons.Default.Computer
            
            // Cloud & Cybersecurity
            "cloud" -> Icons.Default.Cloud
            "cloud_computing" -> Icons.Default.Cloud
            "cybersecurity" -> Icons.Default.Security
            "networking" -> Icons.Default.Hub
            
            // Practical Skills
            "git" -> Icons.Default.AccountTree
            "software_engineering" -> Icons.Default.Engineering
            
            // Fallback for unknown keys
            else -> Icons.Default.School
        }
    }
    
    /**
     * Check if icon key is known/valid.
     * Useful for debugging.
     */
    fun isKnown(iconKey: String?): Boolean {
        return resolve(iconKey) != Icons.Default.School || iconKey?.lowercase()?.trim() == "school"
    }
}
